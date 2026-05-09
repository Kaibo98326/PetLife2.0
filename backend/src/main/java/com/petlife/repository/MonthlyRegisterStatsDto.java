package com.petlife.repository;


import lombok.Data;

@Data
public class MonthlyRegisterStatsDto {
	
	private String month;
	
	private Long count;
	
	 public MonthlyRegisterStatsDto(
	            String month,
	            Long count
	    ) {
	        this.month = month;
	        this.count = count;
	    }

	    public String getMonth() {
	        return month;
	    }

	    public void setMonth(String month) {
	        this.month = month;
	    }

	    public Long getCount() {
	        return count;
	    }

	    public void setCount(Long count) {
	        this.count = count;
	    }
}
