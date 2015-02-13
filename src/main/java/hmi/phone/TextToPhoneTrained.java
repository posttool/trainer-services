/**
 * Copyright 2000-2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package hmi.phone;

import hmi.ml.cart.CART;
import hmi.ml.cart.LeafNode.StringAndFloatLeafNode;
import hmi.ml.feature.FeatureDefinition;
import hmi.ml.feature.FeatureVector;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;


public class TextToPhoneTrained {
	public static final String PREDICTED_STRING_FEATURENAME = "predicted-string";

	private CART tree;
	private FeatureDefinition featureDefinition;
	private int indexPredictedFeature;
	private int context;
	private AllophoneSet allophoneSet;
	private boolean convertToLowercase;
	protected boolean removeTrailingOneFromPhones = true;


	public TextToPhoneTrained(AllophoneSet aPhonSet, InputStream treeStream, boolean removeTrailingOneFromPhones) throws IOException {
		this.allophoneSet = aPhonSet;
		this.loadTree(treeStream);
		this.removeTrailingOneFromPhones = removeTrailingOneFromPhones;
	}

	
	public TextToPhoneTrained(AllophoneSet aPhonSet, InputStream treeStream) throws IOException {
		this(aPhonSet, treeStream, true);
	}

	public TextToPhoneTrained(AllophoneSet aPhonSet, CART predictionTree) {
		this.allophoneSet = aPhonSet;
		this.tree = predictionTree;
		this.featureDefinition = tree.getFeatureDefinition();
		this.indexPredictedFeature = featureDefinition.getFeatureIndex(PREDICTED_STRING_FEATURENAME);
		Properties props = tree.getProperties();
		if (props == null)
			throw new IllegalArgumentException("Prediction tree does not contain properties");
		convertToLowercase = Boolean.parseBoolean(props.getProperty("lowercase"));
		context = Integer.parseInt(props.getProperty("context"));
	}

	
	public void loadTree(InputStream treeStream) throws IOException {
		MaryCARTReader cartReader = new MaryCARTReader();
		this.tree = cartReader.loadFromStream(treeStream);
		this.featureDefinition = tree.getFeatureDefinition();
		this.indexPredictedFeature = featureDefinition.getFeatureIndex(PREDICTED_STRING_FEATURENAME);
		this.convertToLowercase = false;
		Properties props = tree.getProperties();
		if (props == null)
			throw new IllegalArgumentException("Prediction tree does not contain properties");
		convertToLowercase = Boolean.parseBoolean(props.getProperty("lowercase"));
		context = Integer.parseInt(props.getProperty("context"));
	}

	public String predictPronunciation(String graphemes) {
		if (convertToLowercase)
			graphemes = graphemes.toLowerCase(allophoneSet.getLocale());

		String returnStr = "";

		for (int i = 0; i < graphemes.length(); i++) {

			byte[] byteFeatures = new byte[2 * this.context + 1];

			for (int fnr = 0; fnr < 2 * this.context + 1; fnr++) {
				int pos = i - context + fnr;

				String grAtPos = (pos < 0 || pos >= graphemes.length()) ? "null" : graphemes.substring(pos, pos + 1);

				try {
					byteFeatures[fnr] = this.tree.getFeatureDefinition().getFeatureValueAsByte(fnr, grAtPos);
					// ... can also try to call explicit:
					// features[fnr] = this.fd.getFeatureValueAsByte("att"+fnr, cg.substr(pos)
				} catch (IllegalArgumentException iae) {
					// Silently ignore unknown characters
					byteFeatures[fnr] = this.tree.getFeatureDefinition().getFeatureValueAsByte(fnr, "null");
				}
			}

			FeatureVector fv = new FeatureVector(byteFeatures, new short[] {}, new float[] {}, 0);

			StringAndFloatLeafNode leaf = (StringAndFloatLeafNode) tree.interpretToNode(fv, 0);
			String prediction = leaf.mostProbableString(featureDefinition, indexPredictedFeature);
			returnStr += prediction.substring(1, prediction.length() - 1);
		}

		return returnStr;

	}

	public String syllabify(String phones) {
		return allophoneSet.syllabify(phones);
	}

	public static void main(String[] args) throws IOException {

		if (args.length < 2) {
			System.out.println("Usage:");
			System.out
					.println("java TrainedLTS allophones.xml lts-model.lts [removeTrailingOneFromPhones]");
			System.exit(0);
		}
		String allophoneFile = args[0];
		String ltsFile = args[1];
		boolean myRemoveTrailingOneFromPhones = true;
		if (args.length > 2) {
			myRemoveTrailingOneFromPhones = Boolean.getBoolean(args[2]);
		}

		TextToPhoneTrained lts = new TextToPhoneTrained(AllophoneSet.getAllophoneSet(allophoneFile), new FileInputStream(ltsFile),
				myRemoveTrailingOneFromPhones);

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			String pron = lts.predictPronunciation(line);
			String syl = lts.syllabify(pron);
			String sylStripped = syl.replaceAll("[-' ]+", "");
			System.out.println(sylStripped);
		}
	}

}
