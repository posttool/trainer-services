package hmi.ml.cart.io;

import hmi.ml.cart.CART;
import hmi.ml.cart.DecisionNode;
import hmi.ml.cart.DecisionNode.BinaryByteDecisionNode;
import hmi.ml.cart.DecisionNode.BinaryFloatDecisionNode;
import hmi.ml.cart.DecisionNode.BinaryShortDecisionNode;
import hmi.ml.cart.LeafNode;
import hmi.ml.cart.LeafNode.FeatureVectorLeafNode;
import hmi.ml.cart.LeafNode.FloatLeafNode;
import hmi.ml.cart.LeafNode.IntAndFloatArrayLeafNode;
import hmi.ml.cart.LeafNode.IntArrayLeafNode;
import hmi.ml.cart.LeafNode.LeafType;
import hmi.ml.cart.Node;
import hmi.ml.feature.FeatureIO;
import hmi.ml.feature.FeatureVector;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

public class CARTWriter {

    public void dump(CART cart, String destFile) throws IOException {
        if (cart == null)
            throw new NullPointerException("Cannot dump null CART");
        if (destFile == null)
            throw new NullPointerException("No destination file");

        // Open the destination file (cart.bin) and output the header
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destFile)));
        // create new CART-header and write it to output file
        CARTHeader hdr = new CARTHeader(CARTHeader.CARTS);
        hdr.writeTo(out);

        Properties props = cart.getProperties();
        if (props == null) {
            out.writeShort(0);
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            props.store(baos, null);
            byte[] propData = baos.toByteArray();
            out.writeShort(propData.length);
            out.write(propData);
        }

        // feature definition
        FeatureIO.write(cart.getFeatureDefinition(), out);

        // dump CART
        dumpBinary(cart.getRootNode(), out);

        // finish
        out.close();
        // logger.debug(" ... done\n");
    }

    public void toTextOut(CART cart, PrintWriter pw) throws IOException {
        try {
            int id[] = new int[2];
            id[0] = 0; // number of decision nodes
            id[1] = 0; // number of leaf nodes

            // System.out.println("Total number of nodes:" +
            // rootNode.getNumberOfNodes());
            setUniqueNodeId(cart.getRootNode(), id);
            pw.println("Num decision nodes= " + id[0] + "  Num leaf nodes= " + id[1]);
            printDecisionNodes(cart.getRootNode(), null, pw);
            pw.println("\n----------------\n");
            printLeafNodes(cart.getRootNode(), null, pw);

            pw.flush();
            pw.close();
        } catch (IOException ioe) {
            IOException newIOE = new IOException("Error dumping CART to standard output");
            newIOE.initCause(ioe);
            throw newIOE;
        }
    }

    private void setUniqueNodeId(Node node, int id[]) throws IOException {
        // if the node is decision node
        if (node.getNumberOfNodes() > 1) {
            assert node instanceof DecisionNode;
            DecisionNode decNode = (DecisionNode) node;
            id[0]--;
            decNode.setUniqueDecisionNodeId(id[0]);

            // add Ids
            for (int i = 0; i < decNode.getNumberOfChildren(); i++) {
                setUniqueNodeId(decNode.getChild(i), id);
            }

        } else { // the node is a leaf node
            assert node instanceof LeafNode;
            LeafNode leaf = (LeafNode) node;
            if (leaf.isEmpty()) {
                leaf.setUniqueLeafId(0);
            } else {
                id[1]++;
                leaf.setUniqueLeafId(id[1]);
            }
        }

    }

    private void dumpBinary(Node rootNode, DataOutput os) throws IOException {
        try {

            int id[] = new int[2];
            id[0] = 0; // number of decision nodes
            id[1] = 0; // number of leaf nodes
            // first add unique identifiers to decision nodes and leaf nodes
            setUniqueNodeId(rootNode, id);

            // write the number of decision nodes
            os.writeInt(Math.abs(id[0]));
            // lines that start with a negative number are decision nodes
            printDecisionNodes(rootNode, os, null);

            // write the number of leaves.
            os.writeInt(id[1]);
            // lines that start with id are leaf nodes
            printLeafNodes(rootNode, (DataOutputStream) os, null);

        } catch (IOException ioe) {
            IOException newIOE = new IOException("Error dumping CART to output stream");
            newIOE.initCause(ioe);
            throw newIOE;
        }
    }

    private void printDecisionNodes(Node node, DataOutput out, PrintWriter pw) throws IOException {
        if (!(node instanceof DecisionNode))
            return;

        DecisionNode decNode = (DecisionNode) node;
        int id = decNode.getUniqueDecisionNodeId();
        String nodeDefinition = decNode.getNodeDefinition();
        int featureIndex = decNode.getFeatureIndex();
        DecisionNode.Type nodeType = decNode.getDecisionNodeType();

        if (out != null) {
            out.writeInt(featureIndex);
            out.writeInt(nodeType.ordinal());
            switch (nodeType) {
            case BinaryByteDecisionNode:
                out.writeInt(((BinaryByteDecisionNode) decNode).getCriterionValueAsByte());
                assert decNode.getNumberOfChildren() == 2;
                break;
            case BinaryShortDecisionNode:
                out.writeInt(((BinaryShortDecisionNode) decNode).getCriterionValueAsShort());
                assert decNode.getNumberOfChildren() == 2;
                break;
            case BinaryFloatDecisionNode:
                out.writeFloat(((BinaryFloatDecisionNode) decNode).getCriterionValueAsFloat());
                assert decNode.getNumberOfChildren() == 2;
                break;
            case ByteDecisionNode:
            case ShortDecisionNode:
                out.writeInt(decNode.getNumberOfChildren());
            }

            for (int i = 0, n = decNode.getNumberOfChildren(); i < n; i++) {
                Node child = decNode.getChild(i);
                if (child instanceof DecisionNode) {
                    out.writeInt(((DecisionNode) child).getUniqueDecisionNodeId());
                } else {
                    assert child instanceof LeafNode;
                    out.writeInt(((LeafNode) child).getUniqueLeafId());
                }
            }
        }
        if (pw != null) {
            StringBuilder strNode = new StringBuilder(id + " " + nodeDefinition);
            for (int i = 0, n = decNode.getNumberOfChildren(); i < n; i++) {
                strNode.append(" ");
                Node child = decNode.getChild(i);
                if (child instanceof DecisionNode) {
                    strNode.append(((DecisionNode) child).getUniqueDecisionNodeId());
                } else {
                    assert child instanceof LeafNode;
                    strNode.append("id").append(((LeafNode) child).getUniqueLeafId());
                }
            }
            pw.println(strNode.toString());
        }
        for (int i = 0; i < ((DecisionNode) node).getNumberOfChildren(); i++) {
            if (((DecisionNode) node).getChild(i).getNumberOfNodes() > 1)
                printDecisionNodes(((DecisionNode) node).getChild(i), out, pw);
        }
    }

    /**
     * This function will print the leaf nodes only, but it goes through all the
     * decision nodes.
     */
    private void printLeafNodes(Node node, DataOutput out, PrintWriter pw) throws IOException {
        Node nextNode;
        if (node.getNumberOfNodes() > 1) {
            assert node instanceof DecisionNode;
            DecisionNode decNode = (DecisionNode) node;
            for (int i = 0; i < decNode.getNumberOfChildren(); i++) {
                nextNode = decNode.getChild(i);
                printLeafNodes(nextNode, out, pw);
            }
        } else {
            assert node instanceof LeafNode;
            LeafNode leaf = (LeafNode) node;
            if (leaf.getUniqueLeafId() == 0) // empty leaf, do not write
                return;
            LeafType leafType = leaf.getLeafNodeType();
            if (leafType == LeafType.FeatureVectorLeafNode) {
                leafType = LeafType.IntArrayLeafNode;
                // save feature vector leaf nodes as int array leaf nodes
            }
            if (out != null) {
                // Leaf node type
                out.writeInt(leafType.ordinal());
            }
            if (pw != null) {
                pw.print("id" + leaf.getUniqueLeafId() + " " + leafType);
            }
            switch (leaf.getLeafNodeType()) {
            case IntArrayLeafNode:
                int data[] = ((IntArrayLeafNode) leaf).getIntData();
                if (out != null)
                    out.writeInt(data.length);
                if (pw != null)
                    pw.print(" " + data.length);
                for (int i = 0; i < data.length; i++) {
                    if (out != null)
                        out.writeInt(data[i]);
                    if (pw != null)
                        pw.print(" " + data[i]);
                }
                break;
            case FloatLeafNode:
                float stddev = ((FloatLeafNode) leaf).getStDeviation();
                float mean = ((FloatLeafNode) leaf).getMean();
                if (out != null) {
                    out.writeFloat(stddev);
                    out.writeFloat(mean);
                }
                if (pw != null) {
                    pw.print(" 1 " + stddev + " " + mean);
                }
                break;
            case IntAndFloatArrayLeafNode:
            case StringAndFloatLeafNode:
                int data1[] = ((IntAndFloatArrayLeafNode) leaf).getIntData();
                float floats[] = ((IntAndFloatArrayLeafNode) leaf).getFloatData();
                if (out != null)
                    out.writeInt(data1.length);
                if (pw != null)
                    pw.print(" " + data1.length);
                for (int i = 0; i < data1.length; i++) {
                    if (out != null) {
                        out.writeInt(data1[i]);
                        out.writeFloat(floats[i]);
                    }
                    if (pw != null)
                        pw.print(" " + data1[i] + " " + floats[i]);
                }
                break;
            case FeatureVectorLeafNode:
                FeatureVector fv[] = ((FeatureVectorLeafNode) leaf).getFeatureVectors();
                if (out != null)
                    out.writeInt(fv.length);
                if (pw != null)
                    pw.print(" " + fv.length);
                for (int i = 0; i < fv.length; i++) {
                    if (out != null)
                        out.writeInt(fv[i].getUnitIndex());
                    if (pw != null)
                        pw.print(" " + fv[i].getUnitIndex());
                }
                break;
            case PdfLeafNode:
                throw new IllegalArgumentException("Writing of pdf leaf nodes not yet implemented");
            }
            if (pw != null)
                pw.println();
        }
    }
}
