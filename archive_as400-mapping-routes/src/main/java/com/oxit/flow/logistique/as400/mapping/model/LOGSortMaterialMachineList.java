package com.oxit.flow.logistique.as400.mapping.model;

import javax.xml.bind.annotation.*;
import org.apache.camel.Exchange;

import java.util.List;


@XmlRootElement(name = "MT_SortMaterialMachine")
@XmlAccessorType(XmlAccessType.FIELD)
public class LOGSortMaterialMachineList {

  @XmlElement(name = "ZSORT_MAT_MACH", required = true)
  private List<LOGSortMaterialMachine> XmlFieldsModelList;

  public List<LOGSortMaterialMachine> getXmlFieldList() {
    return XmlFieldsModelList;
  }

  public void setListDPILJ(List<LOGSortMaterialMachine> XmlFieldsModelList) {
    this.XmlFieldsModelList = XmlFieldsModelList;
  }

  public void setExchange(Exchange exchange) {
    List<LOGSortMaterialMachine> XmlFieldsModelList =  (List<LOGSortMaterialMachine>) exchange.getIn().getBody();
    this.XmlFieldsModelList = XmlFieldsModelList;
    exchange.getIn().setBody(this);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LOGSortMaterialMachine xmlField:this.getXmlFieldList()){
      xmlField.toFlatFile(sb);
    }
    return sb.toString();
  }

}
//WAREHOUSE_ID,SORTER_TYPE,SORTER_STATUS_ACTIVE,DATE,TIME \n
