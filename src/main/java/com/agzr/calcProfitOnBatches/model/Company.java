package com.agzr.calcProfitOnBatches.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;
import org.openxava.model.*;

import javax.persistence.*;

@Entity @Getter @Setter
public class Company extends Identifiable {

    @SearchKey
    @Required
    @Column(length = 100)
    String name;

    @Column(length = 210)
    String description;

}
