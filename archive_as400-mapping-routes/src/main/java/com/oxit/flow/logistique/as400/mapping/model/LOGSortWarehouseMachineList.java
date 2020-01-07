package com.oxit.flow.logistique.as400.mapping.model;

import javax.xml.bind.annotation.*;
import org.apache.camel.Exchange;

import java.util.List;

@XmlRootElement(name = "MT_LOGSortWarehouseMachine")
@XmlAccessorType(XmlAccessType.FIELD)
public class LOGSortWarehouseMachineList {

  @XmlElement(name = "ZSORT_MACH_WHS", required = true)
  private List<LOGSortWarehouseMachine> XmlFieldsModelList;

  public List<LOGSortWarehouseMachine> getXmlFieldList() {
    return XmlFieldsModelList;
  }

  public void setListDPILJ(List<LOGSortWarehouseMachine> XmlFieldsModelList) {
    this.XmlFieldsModelList = XmlFieldsModelList;
  }

  public void setExchange(Exchange exchange) {
    List<LOGSortWarehouseMachine> XmlFieldsModelList =  (List<LOGSortWarehouseMachine>) exchange.getIn().getBody();
    this.XmlFieldsModelList = XmlFieldsModelList;
    exchange.getIn().setBody(this);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LOGSortWarehouseMachine xmlField:this.getXmlFieldList()){
      xmlField.toFlatFile(sb);
    }
    return sb.toString();
  }

}
//WAREHOUSE_ID,SORTER_TYPE,SORTER_STATUS_ACTIVE,DATE,TIME \n
