package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static final Logger logger = LogManager.getLogger("ParkingDataBaseIT");

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {

    }

    @Test
    public void testParkingACar() {
        //Arrange
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        //Act
        parkingService.processIncomingVehicle();

        //Asserts
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        Assertions.assertNotNull(ticket);

        ParkingSpot spot = ticket.getParkingSpot();
        Assertions.assertNotNull(spot);
        Assertions.assertFalse(spot.isAvailable());
    }

    @Test
    public void testParkingLotExit() {
        //Arrange
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ParkingSpot spot = parkingService.getNextParkingNumberIfAvailable();
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(spot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(0);
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Simulate that the car has been there for 1 hour
        ticket.setOutTime(null);
        ticketDAO.saveTicket(ticket);

        //Act
        parkingService.processExitingVehicle();

        //Assert
        ticket = ticketDAO.getTicket("ABCDEF");
        Assertions.assertNotNull(ticket);
        Assertions.assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
        Assertions.assertNotNull(ticket.getOutTime());
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        //Arrange
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ParkingSpot spot = parkingService.getNextParkingNumberIfAvailable();
        Ticket first_ticket = new Ticket();
        first_ticket.setParkingSpot(spot);
        first_ticket.setVehicleRegNumber("ABCDEF");
        first_ticket.setPrice(Fare.CAR_RATE_PER_HOUR);
        first_ticket.setInTime(new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000))); // Simulate that the car has been there for 1 day
        first_ticket.setOutTime(new Date(System.currentTimeMillis() - (24 *60 * 60 * 1000) + (60 * 60 * 1000))); // Simulate that the car has been there for 1 hour
        ticketDAO.saveTicket(first_ticket);
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(spot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(0);
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Simulate that the car has been there for 1 hour
        ticket.setOutTime(null);
        ticketDAO.saveTicket(ticket);

        //Act
        parkingService.processExitingVehicle();

        //Assert
        ticket = ticketDAO.getTicket("ABCDEF");
        Assertions.assertNotNull(ticket);
        Assertions.assertEquals(Fare.CAR_RATE_PER_HOUR * 0.95, ticket.getPrice());
        Assertions.assertNotNull(ticket.getOutTime());
    }
}
