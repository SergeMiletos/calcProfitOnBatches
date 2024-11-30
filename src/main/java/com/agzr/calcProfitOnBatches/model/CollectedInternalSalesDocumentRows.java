package com.agzr.calcProfitOnBatches.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.model.Identifiable;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@SqlResultSetMapping(name = "queryResultComplex",
        entities = {
                @EntityResult(
                        entityClass = Product.class,
                        fields = {
                                @FieldResult(name = "id", column = "product_id")
                        }
                )
        },
        columns = {
                @ColumnResult(name = "intervalWeight"),
                @ColumnResult(name = "intervalId"),
                @ColumnResult(name = "intervalstart"),
                @ColumnResult(name = "intervalend"),
                @ColumnResult(name = "product_id")
        })
@Entity
@Getter @Setter
public class CollectedInternalSalesDocumentRows extends Identifiable {

    @ManyToOne
    Product product;

    @Column
    LocalDateTime intervalStart;

    @Column
    LocalDateTime intervalEnd;

    @Column
    BigDecimal collectedQuantity;

    @Column
    int intervalId;

    @Column
    int intervalWeight;

}
