package com.oxit.flow.logistique.as400.mapping.model;

import javax.xml.bind.annotation.*;
import org.apache.camel.Exchange;

import java.util.List;


@XmlRootElement(name = "MT_SortGroupStores")
@XmlAccessorType(XmlAccessType.FIELD)
public class LOGSortGroupOfStoresList {

  @XmlElement(name = "ZLLOG_GROUP_STOR", required = true)
  private List<LOGSortGroupOfStores> XmlFieldsModelList;

  public List<LOGSortGroupOfStores> getXmlFieldList() {
    return XmlFieldsModelList;
  }

  public void setListDPILJ(List<LOGSortGroupOfStores> XmlFieldsModelList) {
    this.XmlFieldsModelList = XmlFieldsModelList;
  }

  public void setExchange(Exchange exchange) {
    List<LOGSortGroupOfStores> XmlFieldsModelList =  (List<LOGSortGroupOfStores>) exchange.getIn().getBody();
    this.XmlFieldsModelList = XmlFieldsModelList;
    exchange.getIn().setBody(this);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LOGSortGroupOfStores xmlField:this.getXmlFieldList()){
      xmlField.toFlatFile(sb);
    }
    return sb.toString();
  }

}
//WAREHOUSE_ID,SORTER_TYPE,SORTER_STATUS_ACTIVE,DATE,TIME \n
