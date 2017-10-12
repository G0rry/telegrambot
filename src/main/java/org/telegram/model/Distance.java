package org.telegram.model;

import java.util.Date;
import java.util.List;

/**
 * Created by gorun_pv on 15.02.2017.
 */
public class Distance {
    private int id;
    private City city;
    private String distance;
    private String places;
    private String price;
    private String change;
    private Date dateUpdate;
    private List<PriceIncrease> priceIncreases;

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }



    public Date getDateUpdate() {
        return dateUpdate;
    }

    public void setDateUpdate(Date dateUpdate) {
        this.dateUpdate = dateUpdate;
    }



    public List<PriceIncrease> getPriceIncreases() {
        return priceIncreases;
    }

    public void setPriceIncreases(List<PriceIncrease> priceIncreases) {
        this.priceIncreases = priceIncreases;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public String getDistance() {
        return distance;
    }

    public Distance setDistance(String distance) {
        this.distance = distance;
        return this;
    }

    public String getPlaces() {
        return places;
    }

    public Distance setPlaces(String places) {
        this.places = places;
        return this;
    }

    public String getPrice() {
        return price;
    }

    public Distance setPrice(String price) {
        this.price = price;
        return this;
    }

}
