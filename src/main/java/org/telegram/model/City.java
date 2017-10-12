package org.telegram.model;

import java.util.Date;
import java.util.List;

/**
 * Created by gorun_pv on 15.02.2017.
 */
public class City {
    private int id;
    private String city;
    private String link;
    private Date dateUpdate;
    private List<Distance> distances;

    public Date getDateUpdate() {
        return dateUpdate;
    }

    public void setDateUpdate(Date dateUpdate) {
        this.dateUpdate = dateUpdate;
    }



    public List<Distance> getDistances() {
        return distances;
    }

    public void setDistances(List<Distance> distances) {
        this.distances = distances;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCity() {
        return city;
    }

    public City setCity(String city) {
        this.city = city;
        return this;
    }

    public String getLink() {
        return link;
    }

    public City setLink(String link) {
        this.link = link;
        return this;
    }
}
