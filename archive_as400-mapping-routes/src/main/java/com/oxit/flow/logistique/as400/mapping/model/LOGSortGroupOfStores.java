package com.oxit.flow.logistique.as400.mapping.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ZLLOG_GROUP_STOR")
public class LOGSortGroupOfStores {

    @XmlElement(name = "WAREHOUSE_ID")
    private String WAREHOUSE_ID;
    @XmlElement(name = "ZGROUP_STORES")
    private String ZGROUP_STORES;
    @XmlElement(name = "STORE_EAN")
    private String STORE_EAN;
    @XmlElement(name = "GROUPE_NAME")
    private String GROUPE_NAME;
    @XmlElement(name = "GROUPE_NAME_SHORT")
    private String GROUPE_NAME_SHORT;
    @XmlElement(name = "SENT_DATE")
    private String SENT_DATE;
    
    public void toFlatFile(StringBuilder sb) {
    	String warehaouseId = this.getWAREHOUSE_ID();
    	String groupeStores = this.getZGROUP_STORES();
        String storeEan = this.getSTORE_EAN();
        String groupeName = this.getGROUPE_NAME();
        String groupeNameShort = this.getGROUPE_NAME_SHORT();
        String sentDate = this.getSENT_DATE();

        sb.append(warehaouseId)
          .append("\t")
          .append(groupeStores)
          .append("\t")
          .append(storeEan)
          .append("\t")
          .append(groupeName)
          .append("\t")
          .append(groupeNameShort)
          .append("\t")
          .append(sentDate)
          .append("\r\n");
    }

}
