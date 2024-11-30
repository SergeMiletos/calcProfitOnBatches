package com.agzr.calcProfitOnBatches.model;

import lombok.*;
import org.openxava.annotations.*;
import org.openxava.model.*;

import javax.persistence.*;

@Entity @Getter @Setter
public class Product extends Identifiable {

    @SearchKey
    @Required
    @Column(length = 100)
    String name;

    @Column(length = 210)
    String description;

    @Required
    @Column(length = 5)
    String eom;


}
