package com.oxit.flow.logistique.as400.mapping.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ZSORT_MAT_MACH")
public class LOGSortMaterialMachine {

    @XmlElement(name = "SORTER_TYPE")
    private String SORTER_TYPE;
    @XmlElement(name = "ITEM")
    private String ITEM;
    @XmlElement(name = "SORTABILITY")
    private String SORTABILITY;
    @XmlElement(name = "INJECTION_METHOD")
    private String INJECTION_METHOD;
    @XmlElement(name = "DATE")
    private String DATE;
    @XmlElement(name = "TIME")
    private String TIME;
    
    public void toFlatFile(StringBuilder sb) {
    	String item = this.getITEM();
    	String sortability = this.getSORTABILITY();
        String sorterType = this.getSORTER_TYPE();
        String injectionMethod = this.getINJECTION_METHOD();
        String date = this.getDATE();
        String time = this.getTIME();

        sb.append(sorterType)
          .append("\t")
          .append(item)
          .append("\t")
          .append(sortability)
          .append("\t")
          .append(injectionMethod)
          .append("\t")
          .append(date)
          .append("\t")
          .append(time)
          .append("\r\n");
    }

}
