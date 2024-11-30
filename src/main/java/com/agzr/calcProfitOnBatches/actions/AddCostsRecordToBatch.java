package com.agzr.calcProfitOnBatches.actions;

import com.agzr.calcProfitOnBatches.model.BatchOfGood;
import org.openxava.actions.OnChangePropertyBaseAction;
import java.util.Set;

public class AddCostsRecordToBatch extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        BatchOfGood currentRecord = (BatchOfGood) getView().getEntity();
        BatchOfGood batchRecord = currentRecord.getCostsTargetBatch();

        if (batchRecord != null) {
            if (!batchRecord.getBatchDirectCostRecords().contains(currentRecord)) {
                batchRecord.getBatchDirectCostRecords().add(currentRecord);
            }
        }
    }
}
