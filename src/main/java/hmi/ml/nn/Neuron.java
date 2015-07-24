package hmi.ml.nn;


import java.util.Map;

public class Neuron {
    String ID;
    String label;

    public Neuron() {
        this.ID = null;//Neuron.uid();
        this.label = null;
        Map<String, Connection> inputs;
        Map<String, Connection> projected;
        Map<String, Connection> gated;
        float errorResponsibility = 0;
        float errorProjected = 0;
        float errorGated = 0;
//        this.trace = {
//                elegibility: {},
//        extended: {},
//        influences: {}
//        };
//        this.state = 0;
//        this.old = 0;
//        this.activation = 0;
//        this.selfconnection = new Neuron.connection(this, this, 0); // weight = 0 -> not connected
//        this.squash = Neuron.squash.LOGISTIC;
//        this.neighboors = {};
//        this.bias = Math.random() * .2 - .1;
    }
//
//    Neuron.prototype = {
//
//        // activate the neuron
//        activate: function(input) {
//            // activation from enviroment (for input neurons)
//            if (typeof input != 'undefined') {
//                this.activation = input;
//                this.derivative = 0;
//                this.bias = 0;
//                return this.activation;
//            }
//
//            // old state
//            this.old = this.state;
//
//            // eq. 15
//            this.state = this.selfconnection.gain * this.selfconnection.weight *
//                    this.state + this.bias;
//
//            for (var i in this.connections.inputs) {
//                var input = this.connections.inputs[i];
//                this.state += input.from.activation * input.weight * input.gain;
//            }
//
//            // eq. 16
//            this.activation = this.squash(this.state);
//
//            // f'(s)
//            this.derivative = this.squash(this.state, true);
//
//            // update traces
//            var influences = [];
//            for (var id in this.trace.extended) {
//                // extended elegibility trace
//                var xtrace = this.trace.extended[id];
//                var neuron = this.neighboors[id];
//
//                // if gated neuron's selfconnection is gated by this unit, the influence keeps track of the neuron's old state
//                var influence = neuron.selfconnection.gater == this ? neuron.old : 0;
//
//                // index runs over all the incoming connections to the gated neuron that are gated by this unit
//                for (var incoming in this.trace.influences[neuron.ID]) { // captures the effect that has an input connection to this unit, on a neuron that is gated by this unit
//                    influence += this.trace.influences[neuron.ID][incoming].weight *
//                            this.trace.influences[neuron.ID][incoming].from.activation;
//                }
//                influences[neuron.ID] = influence;
//            }
//
//            for (var i in this.connections.inputs) {
//                var input = this.connections.inputs[i];
//
//                // elegibility trace - Eq. 17
//                this.trace.elegibility[input.ID] = this.selfconnection.gain * this.selfconnection
//                        .weight * this.trace.elegibility[input.ID] + input.gain * input.from
//                        .activation;
//
//                for (var id in this.trace.extended) {
//                    // extended elegibility trace
//                    var xtrace = this.trace.extended[id];
//                    var neuron = this.neighboors[id];
//                    var influence = influences[neuron.ID];
//
//                    // eq. 18
//                    xtrace[input.ID] = neuron.selfconnection.gain * neuron.selfconnection
//                            .weight * xtrace[input.ID] + this.derivative * this.trace.elegibility[
//                            input.ID] * influence;
//                }
//            }
//
//            //  update gated connection's gains
//            for (var connection in this.connections.gated) {
//                this.connections.gated[connection].gain = this.activation;
//            }
//
//            return this.activation;
//        },
//
//        // back-propagate the error
//        propagate: function(rate, target) {
//            // error accumulator
//            var error = 0;
//
//            // whether or not this neuron is in the output layer
//            var isOutput = typeof target != 'undefined';
//
//            // output neurons get their error from the enviroment
//            if (isOutput)
//                this.error.responsibility = this.error.projected = target - this.activation; // Eq. 10
//
//            else // the rest of the neuron compute their error responsibilities by backpropagation
//            {
//                // error responsibilities from all the connections projected from this neuron
//                for (var id in this.connections.projected) {
//                var connection = this.connections.projected[id];
//                var neuron = connection.to;
//                // Eq. 21
//                error += neuron.error.responsibility * connection.gain * connection.weight;
//            }
//
//                // projected error responsibility
//                this.error.projected = this.derivative * error;
//
//                error = 0;
//                // error responsibilities from all the connections gated by this neuron
//                for (var id in this.trace.extended) {
//                var neuron = this.neighboors[id]; // gated neuron
//                var influence = neuron.selfconnection.gater == this ? neuron.old : 0; // if gated neuron's selfconnection is gated by this neuron
//
//                // index runs over all the connections to the gated neuron that are gated by this neuron
//                for (var input in this.trace.influences[id]) { // captures the effect that the input connection of this neuron have, on a neuron which its input/s is/are gated by this neuron
//                    influence += this.trace.influences[id][input].weight * this.trace.influences[
//                            neuron.ID][input].from.activation;
//                }
//                // eq. 22
//                error += neuron.error.responsibility * influence;
//            }
//
//                // gated error responsibility
//                this.error.gated = this.derivative * error;
//
//                // error responsibility - Eq. 23
//                this.error.responsibility = this.error.projected + this.error.gated;
//            }
//
//            // learning rate
//            rate = rate || .1;
//
//            // adjust all the neuron's incoming connections
//            for (var id in this.connections.inputs) {
//                var input = this.connections.inputs[id];
//
//                // Eq. 24
//                var gradient = this.error.projected * this.trace.elegibility[input.ID];
//                for (var id in this.trace.extended) {
//                    var neuron = this.neighboors[id];
//                    gradient += neuron.error.responsibility * this.trace.extended[
//                            neuron.ID][input.ID];
//                }
//                input.weight += rate * gradient; // adjust weights - aka learn
//            }
//
//            // adjust bias
//            this.bias += rate * this.error.responsibility;
//        },
//
//        project: function(neuron, weight) {
//            // self-connection
//            if (neuron == this) {
//                this.selfconnection.weight = 1;
//                return this.selfconnection;
//            }
//
//            // check if connection already exists
//            var connected = this.connected(neuron);
//            if (connected && connected.type == "projected") {
//                // update connection
//                if (typeof weight != 'undefined')
//                connected.connection.weight = weight;
//                // return existing connection
//                return connected.connection;
//            } else {
//                // create a new connection
//                var connection = new Neuron.connection(this, neuron, weight);
//            }
//
//            // reference all the connections and traces
//            this.connections.projected[connection.ID] = connection;
//            this.neighboors[neuron.ID] = neuron;
//            neuron.connections.inputs[connection.ID] = connection;
//            neuron.trace.elegibility[connection.ID] = 0;
//
//            for (var id in neuron.trace.extended) {
//                var trace = neuron.trace.extended[id];
//                trace[connection.ID] = 0;
//            }
//
//            return connection;
//        },
//
//        gate: function(connection) {
//            // add connection to gated list
//            this.connections.gated[connection.ID] = connection;
//
//            var neuron = connection.to;
//            if (!(neuron.ID in this.trace.extended)) {
//                // extended trace
//                this.neighboors[neuron.ID] = neuron;
//                var xtrace = this.trace.extended[neuron.ID] = {};
//                for (var id in this.connections.inputs) {
//                    var input = this.connections.inputs[id];
//                    xtrace[input.ID] = 0;
//                }
//            }
//
//            // keep track
//            if (neuron.ID in this.trace.influences)
//            this.trace.influences[neuron.ID].push(connection);
//            else
//            this.trace.influences[neuron.ID] = [connection];
//
//            // set gater
//            connection.gater = this;
//        },
//
//        // returns true or false whether the neuron is self-connected or not
//        selfconnected: function() {
//            return this.selfconnection.weight !== 0;
//        },
//
//        // returns true or false whether the neuron is connected to another neuron (parameter)
//        connected: function(neuron) {
//            var result = {
//                    type: null,
//                    connection: false
//            };
//
//            if (this == neuron) {
//                if (this.selfconnected()) {
//                    result.type = 'selfconnection';
//                    result.connection = this.selfconnection;
//                    return result;
//                } else
//                    return false;
//            }
//
//            for (var type in this.connections) {
//                for (var connection in this.connections[type]) {
//                    var connection = this.connections[type][connection];
//                    if (connection.to == neuron) {
//                        result.type = type;
//                        result.connection = connection;
//                        return result;
//                    } else if (connection.from == neuron) {
//                        result.type = type;
//                        result.connection = connection;
//                        return result;
//                    }
//                }
//            }
//
//            return false;
//        },
//
//        // clears all the traces (the neuron forgets it's context, but the connections remain intact)
//        clear: function() {
//
//            for (var trace in this.trace.elegibility)
//            this.trace.elegibility[trace] = 0;
//
//            for (var trace in this.trace.extended)
//            for (var extended in this.trace.extended[trace])
//            this.trace.extended[trace][extended] = 0;
//
//            this.error.responsibility = this.error.projected = this.error.gated = 0;
//        },
//
//        // all the connections are randomized and the traces are cleared
//        reset: function() {
//            this.clear();
//
//            for (var type in this.connections)
//            for (var connection in this.connections[type])
//            this.connections[type][connection].weight = Math.random() * .2 - .1;
//            this.bias = Math.random() * .2 - .1;
//
//            this.old = this.state = this.activation = 0;
//        },

}
