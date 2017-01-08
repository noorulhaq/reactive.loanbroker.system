package com.reactive.loanbroker.model;

public class Quotation {



    private  String bank;

    private  Double offer;

    public Quotation() {}


    public Quotation(String bank, Double offer) {
        this.bank = bank;
        this.offer = offer;
    }

    public String getBank() {
        return bank;
    }

    public Double getOffer() {
        return offer;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public void setOffer(Double offer) {
        this.offer = offer;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Quotation quotation = (Quotation) o;

        if (getBank() != null ? !getBank().equals(quotation.getBank()) : quotation.getBank() != null) return false;
        return getOffer() != null ? getOffer().equals(quotation.getOffer()) : quotation.getOffer() == null;
    }

    @Override
    public int hashCode() {
        int result = getBank() != null ? getBank().hashCode() : 0;
        result = 31 * result + (getOffer() != null ? getOffer().hashCode() : 0);
        return result;
    }




    @Override
    public String toString() {
        return bank+" offer is: "+offer;
    }
}
