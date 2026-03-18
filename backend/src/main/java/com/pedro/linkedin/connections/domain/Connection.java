package com.pedro.linkedin.connections.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(
        name = "connections",
        indexes = {
                @Index(name = "idx_connection_company", columnList = "company"),
                @Index(name = "idx_connection_position", columnList = "position"),
                @Index(name = "idx_connection_connected_on", columnList = "connected_on")
        }
)
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "company")
    private String company;

    @Column(name = "position")
    private String position;

    @Column(name = "connected_on")
    private LocalDate connectedOn;

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public LocalDate getConnectedOn() {
        return connectedOn;
    }

    public void setConnectedOn(LocalDate connectedOn) {
        this.connectedOn = connectedOn;
    }
}
