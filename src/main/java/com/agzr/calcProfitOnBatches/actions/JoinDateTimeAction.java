package com.agzr.calcProfitOnBatches.actions;

import org.openxava.actions.OnChangePropertyBaseAction;

import java.time.*;

public class JoinDateTimeAction extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        LocalTime localEmptyTime = LocalTime.of(0, 0, 0, 0);
        LocalDate localDate = (LocalDate) getView().getValue("positionDate");

        if (localDate != null) {
            getView().setValue("momentOfTimePosition",
                    LocalDateTime.of((LocalDate) getView().getValue("positionDate"),
                            getView().getValue("positionTime") == null ? localEmptyTime
                                    : (LocalTime) getView().getValue("positionTime")));
        }
    }
}
