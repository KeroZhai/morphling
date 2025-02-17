package com.keroz.morphling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.keroz.morphling.annotation.Mapping;
import com.keroz.morphling.mapper.Mapper;
import com.keroz.morphling.mapper.MapperFactory;

import lombok.Data;
import lombok.EqualsAndHashCode;

public class MappingTest {

    private MapperFactory mapperFactory = MapperFactory.defaultMapperFactory();

    @Data
    public static class Source1 {
        private String a = "123";
    }

    @Data
    public static class Target1 {
        @Mapping(alias = "a")
        private String b;
    }

    @Test
    public void testAlias() {
        Source1 source = new Source1();
        Mapper<Source1, Target1> mapper = mapperFactory.getMapperFor(Source1.class, Target1.class);
        Target1 target = mapper.map(source);

        assertEquals(source.getA(), target.getB());
    }

    public static class TimestampToDateConverter implements com.keroz.morphling.converter.Converter<Long, Date> {

        @Override
        public Date convert(Long source, MapperFactory mapperFactory) {
            return new Date(source);
        }

    }

    @Data
    public static class Source2 {
        private long timestamp = new Date().getTime();
    }

    @Data
    public static class Target2 {
        @Mapping(alias = "timestamp", converter = TimestampToDateConverter.class)
        private Date date;
    }

    @Test
    public void testConverter() {
        Source2 source = new Source2();
        Mapper<Source2, Target2> mapper = mapperFactory.getMapperFor(Source2.class, Target2.class);
        Target2 target = mapper.map(source);

        assertEquals(new Date(source.getTimestamp()), target.getDate());
    }

    @Data
    public static class Source3 {
        private List<String> stringList;
    }

    @Data
    public static class Target3 {
        @Mapping(initialValueType = ArrayList.class)
        private List<String> stringList;
    }

    @Test
    public void testInitialValueType() {
        Source3 source = new Source3();
        source.setStringList(Arrays.asList("123"));
        Mapper<Source3, Target3> mapper = mapperFactory.getMapperFor(Source3.class, Target3.class);
        Target3 target = mapper.map(source);

        assertEquals(source.getStringList(), target.getStringList());
    }

    @Data
    public static abstract class Vehicle {
        private String brand;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Car extends Vehicle {
        private int numberOfDoors;
        private String model;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Truck extends Vehicle {
        private int numberOfWheels;
    }

    @Data
    public static abstract class VehicleDto {
        private String brand;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CarDto extends VehicleDto {
        private int numberOfDoors;
        private String model;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TruckDto extends VehicleDto {
        private int numberOfWheels;
    }

    @Data
    public static class ParkingLot {
        private List<Vehicle> vehicles;
    }

    @Data
    public static class ParkingLotDto {
        @Mapping(initialValueType = ArrayList.class)
        private List<@Mapping.Generic(initialValueTypeMappings = {
                @Mapping.InitialValueTypeMapping(sourceType = Car.class, targetType = CarDto.class),
                @Mapping.InitialValueTypeMapping(sourceType = Truck.class, targetType = TruckDto.class)
        }) VehicleDto> vehicles;
    }

    @Test
    public void testInitialValueTypeMappings() {
        ParkingLot parkingLot = new ParkingLot();

        Car car = new Car();
        car.setBrand("Toyota");
        car.setModel("Corolla");
        car.setNumberOfDoors(4);

        Truck truck = new Truck();
        truck.setBrand("Ford");
        truck.setNumberOfWheels(4);

        parkingLot.setVehicles(Arrays.asList(car, truck));

        Mapper<ParkingLot, ParkingLotDto> mapper = mapperFactory.getMapperFor(ParkingLot.class, ParkingLotDto.class);
        ParkingLotDto parkingLotDto = mapper.map(parkingLot);

        assertEquals(2, parkingLotDto.getVehicles().size());
        assertEquals(CarDto.class, parkingLotDto.getVehicles().get(0).getClass());
        assertEquals(TruckDto.class, parkingLotDto.getVehicles().get(1).getClass());

        assertEquals("Toyota", parkingLotDto.getVehicles().get(0).getBrand());
        assertEquals("Corolla", ((CarDto) parkingLotDto.getVehicles().get(0)).getModel());
        assertEquals(4, ((CarDto) parkingLotDto.getVehicles().get(0)).getNumberOfDoors());

        assertEquals("Ford", parkingLotDto.getVehicles().get(1).getBrand());
        assertEquals(4, ((TruckDto) parkingLotDto.getVehicles().get(1)).getNumberOfWheels());
    }

}
