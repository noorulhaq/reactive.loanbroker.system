package com.reactive.bank.model;

public class Quotation {

    private  String bank;

    private  Double offer;

    public String getBank() {
        return bank;
    }

    public Double getOffer() {
        return offer;
    }

    public Quotation(String bank, Double offer) {
        this.bank = bank;
        this.offer = offer;
    }

}
