package com.petlife.model;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity @Table(name = "Pet")
@Data
public class Pet {
	 	@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "pet_id")
	    private Integer petId;
	 	
	 	@ManyToOne
	    @JoinColumn(name = "member_id")
	    private Member member;

	    @Column(name = "pet_name")
	    private String petName;

	    @Column(name = "breed")
	    private String breed;

	    @Column(name = "species")
	    private String species;

	    @Column(name = "age")
	    private Integer age;

	    @Column(name = "weight")
	    private Double weight;

	    @Column(name = "medical_history")
	    private String medicalHistory;

	    @Column(name = "pet_photo")
	    private String petPhoto;

	    @Column(name = "status")
	    private String status = "active";
}
