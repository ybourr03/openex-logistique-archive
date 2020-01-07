package com.oxit.flow.logistique.logecom.as400totwist.model;


import java.util.Arrays;
import java.util.List;

public class As400ToTwistConfiguration {

    private String name;
    private String inputQueue;
    private List<String> listAS400;
    private String queue;

    public As400ToTwistConfiguration(String inputQueue, String name, String listAS400, String queue) {
        this.inputQueue = inputQueue;
        this.name = name;
        this.listAS400 = Arrays.asList(listAS400.split(";"));
        this.queue = queue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getListAS400() {
        return listAS400;
    }

    public void setListAS400(List<String> listAS400) {
        this.listAS400 = listAS400;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getInputQueue() {
        return inputQueue;
    }

    public void setInputQueue(String inputQueue) {
        this.inputQueue = inputQueue;
    }


}
