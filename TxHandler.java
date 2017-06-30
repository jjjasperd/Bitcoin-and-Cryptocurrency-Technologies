import java.util.ArrayList;
import java.util.List;

public class TxHandler {

    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        double inputval = 0.0;
        double outputval = 0.0;

        //(1) all outputs claimed by {@code tx} are in the current UTXO pool,
        for (Transaction.Input input : inputs){
            UTXO u = new UTXO(input.prevTxHash,input.outputIndex);
            if (!this.utxoPool.contains(u))
                return false;
        }
        //(2) the signatures on each input of {@code tx} are valid,
        for (int i=0; i<tx.numInputs();i++){
            UTXO u = new UTXO(inputs.get(i).prevTxHash,inputs.get(i).outputIndex);
            Transaction.Output output = this.utxoPool.getTxOutput(u);
            if (!Crypto.verifySignature(output.address,tx.getRawDataToSign(i),inputs.get(i).signature))
                return false;
        }
        //(3) no UTXO is claimed multiple times by {@code tx},
        List<UTXO> claimed = new ArrayList<>();
        for (Transaction.Input input : inputs){
            UTXO u = new UTXO(input.prevTxHash,input.outputIndex);
            if (claimed.contains(u))
                return false;
            claimed.add(u);
        }
        //(4) all of {@code tx}s output values are non-negative,
        for (Transaction.Output output:tx.getOutputs()){
            if (output.value < 0.0){
                return false;
            }

            outputval+=output.value;
        }

        //(5) the sum of {@code tx}s input values is greater than or equal to the sum of its output values
        for (Transaction.Input input:tx.getInputs()){
            UTXO u = new UTXO(input.prevTxHash,input.outputIndex);
            inputval += this.utxoPool.getTxOutput(u).value;
        }
        if (inputval<outputval){
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        int currentValidNum = 0;
        int lastValidNum = 0;
        List<Transaction> res = new ArrayList<>();
        do {
            lastValidNum = currentValidNum;
            for (Transaction next : possibleTxs) {
                if (isValidTx(next)) {
                    for (Transaction.Input in : next.getInputs()) {
                        UTXO del = new UTXO(in.prevTxHash, in.outputIndex);
                        utxoPool.removeUTXO(del);
                    }

                    int index = 0;
                    for (Transaction.Output out : next.getOutputs()) {
                        UTXO add = new UTXO(next.getHash(), index);
                        index++;
                        utxoPool.addUTXO(add, out);
                    }
                    currentValidNum++;
                    res.add(next);
                }
            }
        }while(currentValidNum != lastValidNum);
        Transaction[] resArr = new Transaction[res.size()];
        resArr = res.toArray(resArr);
        return resArr;
    }

}
