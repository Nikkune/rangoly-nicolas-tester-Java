package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
        try {
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processIncomingVehicleTest() throws Exception {
        // Arrange
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(inputReaderUtil.readSelection()).thenReturn(1);

        // Act
        parkingService.processIncomingVehicle();

        // Assert
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    public void processIncomingVehicleTestRegularUser() throws Exception {
        // Arrange
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(inputReaderUtil.readSelection()).thenReturn(1);

        // Act
        parkingService.processIncomingVehicle();

        // Assert
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
    }

    @Test
    public void processIncomingVehicleTestUnableProcess() throws Exception {
        // Arrange
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(inputReaderUtil.readSelection()).thenReturn(1);
        doThrow(Exception.class).when(inputReaderUtil).readVehicleRegistrationNumber();

        // Act
        parkingService.processIncomingVehicle();

        // Assert
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void processExitingVehicleTest() throws Exception {
        // Arrange
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        // Act
        parkingService.processExitingVehicle();

        // Assert
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        assertEquals((1 * Fare.CAR_RATE_PER_HOUR), ticket.getPrice());
    }

    @Test
    public void processExitingVehicleTestRegularUser() throws Exception {
        // Arrange
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (45 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        // Act
        parkingService.processExitingVehicle();

        // Assert
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
        assertEquals((0.75 * Fare.CAR_RATE_PER_HOUR) * 0.95, ticket.getPrice());
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        // Arrange
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        // Act
        parkingService.processExitingVehicle();

        // Assert
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
    }

    @Test
    public void getNextParkingNumberIfAvailableTest() {
        // Arrange
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(inputReaderUtil.readSelection()).thenReturn(2);

        // Act
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getId());
        Assertions.assertEquals(ParkingType.BIKE, result.getParkingType());
    }

    @Test
    public void getNextParkingNumberIfAvailableTestParkingNumberNotFound() {
        // Arrange
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);
        when(inputReaderUtil.readSelection()).thenReturn(1);

        // Act
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        // Assert
        Assertions.assertNull(result);
    }

    @Test
    public void getNextParkingNumberIfAvailableTestParkingNumberWrongArgument() {
        // Arrange
        when(inputReaderUtil.readSelection()).thenReturn(3);

        // Act
        ParkingSpot result = parkingService.getNextParkingNumberIfAvailable();

        // Assert
        Assertions.assertNull(result);
    }
}
