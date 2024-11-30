package com.agzr.calcProfitOnBatches.model;

import com.agzr.calcProfitOnBatches.actions.JoinDateTimeAction;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.openxava.annotations.*;
import org.openxava.model.Identifiable;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Getter @Setter
public class SalesRecords extends Identifiable {

    @Column
    @ReadOnly
    LocalDateTime momentOfTimePosition;

    @OnChange(JoinDateTimeAction.class)
    @Formula("CAST(MOMENTOFTIMEPOSITION AS DATE)")
    LocalDate positionDate;

    @OnChange(JoinDateTimeAction.class)
    @Formula("CAST(MOMENTOFTIMEPOSITION AS TIME)")
    LocalTime positionTime;

    @ManyToOne
    Product recordProduct;

    @ManyToOne
    Company recordCompany;

    @Column(length = 100)
    @Required
    String salesDocumentId;

    @Column
    @Digits(integer = 11, fraction = 3)
    BigDecimal quantity;

    @Column
    @Digits(integer = 11, fraction = 3)
    @Money
    BigDecimal recordAmount;

}
