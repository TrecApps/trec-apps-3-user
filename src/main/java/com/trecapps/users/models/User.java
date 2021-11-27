package com.trecapps.users.models;

import javax.persistence.*;

@Entity
@Table
public class User
{
    @Id
    String id;

    @Column(unique = true)
    String username;

}
