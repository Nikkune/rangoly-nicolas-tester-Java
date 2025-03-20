package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        long durationInMillis = outHour - inHour;
        float duration = (float) durationInMillis / 1000 / 60 / 60;
        duration = BigDecimal.valueOf(duration).setScale(2, RoundingMode.HALF_UP).floatValue();


        if (duration <= 0.5) {
            duration = 0;
        }
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                if (discount)
                    ticket.setPrice((duration * Fare.CAR_RATE_PER_HOUR) * 0.95);
                else
                    ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                if (discount)
                    ticket.setPrice((duration * Fare.BIKE_RATE_PER_HOUR) * 0.95);
                else
                    ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default:
                throw new IllegalArgumentException("Unkown Parking Type");
        }
    }

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}