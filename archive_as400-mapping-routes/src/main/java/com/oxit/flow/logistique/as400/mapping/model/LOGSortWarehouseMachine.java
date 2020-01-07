package com.oxit.flow.logistique.as400.mapping.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ZSORT_MACH_WHS")
public class LOGSortWarehouseMachine {

    @XmlElement(name = "SORTER_TYPE")
    private String SORTER_TYPE;
    @XmlElement(name = "WAREHOUSE_ID")
    private String WAREHOUSE_ID;
    @XmlElement(name = "SORTER_STATUS_ACTIVE")
    private String SORTER_STATUS_ACTIVE;
    @XmlElement(name = "DATE")
    private String DATE;
    @XmlElement(name = "TIME")
    private String TIME;
    
    public void toFlatFile(StringBuilder sb) {
    	String warehouseID = this.getWAREHOUSE_ID();
        String sorterType = this.getSORTER_TYPE();
        String sorter_status_active = this.getSORTER_STATUS_ACTIVE();
        String date = this.getDATE();
        String time = this.getTIME();

        sb.append(warehouseID)
          .append("\t")
          .append(sorterType)
          .append("\t")
          .append(sorter_status_active)
          .append("\t")
          .append(date)
          .append("\t")
          .append(time)
          .append("\r\n");
    }

}
