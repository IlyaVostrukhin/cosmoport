package com.space.service;

import com.space.controller.ShipOrder;
import com.space.exceptions.BadRequestException;
import com.space.exceptions.NotFoundException;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShipService {
    @Autowired
    ShipRepo shipRepo;

    public List<Ship> getSortedShips(List<Ship> ships, Integer pageNumber, Integer pageSize, String order) {
        if (order == null && pageNumber == null && pageSize == null)
            return ships.stream().limit(3).collect(Collectors.toList());

        if (pageSize != null && order == null && pageNumber == null)
            return ships.stream().limit(pageSize).collect(Collectors.toList());

        if (pageNumber != null && order == null && pageSize == null)
            return ships.stream().skip(pageNumber * 3).limit(3).collect(Collectors.toList());

        if (pageSize == null && pageNumber == null)
            return ships.stream().sorted(getComparatorByOrder(order)).limit(3).collect(Collectors.toList());

        if (pageSize == null)
            return ships.stream().sorted(getComparatorByOrder(order)).limit(3).collect(Collectors.toList());

        if (pageNumber == null)
            return ships.stream().sorted(getComparatorByOrder(order)).limit(pageSize).skip(pageSize).collect(Collectors.toList());

        return ships.stream().sorted(getComparatorByOrder(order)).skip(pageNumber * pageSize).limit(pageSize).collect(Collectors.toList());
    }

    private Comparator<Ship> getComparatorByOrder(String order) {
        switch (ShipOrder.valueOf(order)) {
            case ID:
                return Comparator.comparing(Ship::getId);
            case SPEED:
                return Comparator.comparing(Ship::getSpeed);
            case DATE:
                return Comparator.comparing(Ship::getProdDate);
            case RATING:
                return Comparator.comparing(Ship::getRating);
            default:
                return Comparator.comparing(Ship::getId);
        }
    }

    public List<Ship> getFilteredShips(String name,
                                       String planet,
                                       String shipType,
                                       Long after,
                                       Long before,
                                       Boolean isUsed,
                                       Double minSpeed,
                                       Double maxSpeed,
                                       Integer minCrewSize,
                                       Integer maxCrewSize,
                                       Double minRating,
                                       Double maxRating) {
        List<Ship> ships = shipRepo.findAll();

        if (name != null)
            ships = ships.stream().filter(ship -> ship.getName().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());

        if (planet != null)
            ships = ships.stream().filter(ship -> ship.getPlanet().toLowerCase().contains(planet.toLowerCase())).collect(Collectors.toList());

        if (shipType != null)
            ships = ships.stream().filter(ship -> ship.getShipType().equals(ShipType.valueOf(shipType))).collect(Collectors.toList());

        if (after != null)
            ships = ships.stream().filter(ship -> ship.getProdDate().after(new Date(after))).collect(Collectors.toList());

        if (before != null)
            ships = ships.stream().filter(ship -> ship.getProdDate().before(new Date(before))).collect(Collectors.toList());

        if (isUsed != null)
            ships = ships.stream().filter(ship -> ship.isUsed().equals(isUsed)).collect(Collectors.toList());

        if (minSpeed != null)
            ships = ships.stream().filter(ship -> ship.getSpeed() >= minSpeed).collect(Collectors.toList());

        if (maxSpeed != null)
            ships = ships.stream().filter(ship -> ship.getSpeed() <= maxSpeed).collect(Collectors.toList());

        if (minCrewSize != null)
            ships = ships.stream().filter(ship -> ship.getCrewSize() >= minCrewSize).collect(Collectors.toList());

        if (maxCrewSize != null)
            ships = ships.stream().filter(ship -> ship.getCrewSize() <= maxCrewSize).collect(Collectors.toList());

        if (minRating != null)
            ships = ships.stream().filter(ship -> ship.getRating() >= minRating).collect(Collectors.toList());

        if (maxRating != null)
            ships = ships.stream().filter(ship -> ship.getRating() <= maxRating).collect(Collectors.toList());

        return ships;
    }

    public Ship createShip(Ship ship) {
        if (ship == null) return null;

        Ship createdShip = new Ship();

        if (ship.isUsed() == null) createdShip.setUsed(false);
        else createdShip.setUsed(ship.isUsed());

        if (ship.getName() == null
                || ship.getPlanet() == null
                || ship.getShipType() == null
                || ship.getProdDate() == null
                || ship.getSpeed() == null
                || ship.getCrewSize() == null)
            throw new BadRequestException();

        if (ship.getName().length() > 50 || ship.getName().equals("") || ship.getPlanet().length() > 50 || ship.getPlanet().equals("") ||
                ship.getSpeed() < 0.01 || ship.getSpeed() > 0.99 || ship.getCrewSize() < 1 || ship.getCrewSize() > 9999)
            throw new BadRequestException();

        if (ship.getProdDate().getTime() < 0)
            throw new BadRequestException();

        Calendar dateMinCal = Calendar.getInstance();
        Calendar dateMaxCal = Calendar.getInstance();

        dateMinCal.set(2800, Calendar.JANUARY, 1);
        dateMaxCal.set(3019, Calendar.DECEMBER, 31);

        if (ship.getProdDate().getTime() < dateMinCal.getTimeInMillis()
                || ship.getProdDate().getTime() > dateMaxCal.getTimeInMillis())
            throw new BadRequestException();

        createdShip.setName(ship.getName());
        createdShip.setPlanet(ship.getPlanet());
        createdShip.setShipType(ship.getShipType());
        createdShip.setProdDate(ship.getProdDate());
        createdShip.setSpeed(ship.getSpeed());
        createdShip.setCrewSize(ship.getCrewSize());
        createdShip.setRating(calcRating(ship));

        return createdShip;
    }

    public Double calcRating(Ship ship) {
        if (ship.getProdDate() != null && ship.getSpeed() != null) {
            boolean isUsed = false;

            if (ship.isUsed() != null)
                isUsed = ship.isUsed();

            double k = isUsed ? 0.5 : 1D;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(ship.getProdDate().getTime());
            int year = calendar.get(Calendar.YEAR);

            return (double) Math.round((80 * ship.getSpeed() * k) / (3019 - year + 1) * 100) / 100;
        }
        return null;
    }

    public Ship getShipById(Long id) {
        if (id == 0)
            throw new BadRequestException();
        return shipRepo.findAll().stream().filter(ship -> ship.getId().equals(id)).findFirst().orElseThrow(NotFoundException::new);
    }

    public boolean updateShip(Ship ship, Ship shipJSON) {
        if (shipJSON.getName() == null && shipJSON.getPlanet() == null && shipJSON.getShipType() == null &&
                shipJSON.getCrewSize() == null && shipJSON.getSpeed() == null && shipJSON.getProdDate() == null)
            return false;

        if (ship == null) return false;

        if (shipJSON.getName() != null) {
            if (shipJSON.getName().length() > 50 || shipJSON.getName().equals("")) throw new BadRequestException();
            ship.setName(shipJSON.getName());
        }

        if (shipJSON.getPlanet() != null) {
            if (shipJSON.getPlanet().length() > 50 || shipJSON.getPlanet().equals("")) throw new BadRequestException();
            ship.setPlanet(shipJSON.getPlanet());
        }

        if (shipJSON.getShipType() != null)
            ship.setShipType(shipJSON.getShipType());

        if (shipJSON.getProdDate() != null) {
            if (shipJSON.getProdDate().getTime() < 0) throw new BadRequestException();
            ship.setProdDate(shipJSON.getProdDate());
        }

        if (shipJSON.isUsed() != null) {
            ship.setUsed(shipJSON.isUsed());
        } else {
            ship.setUsed(false);
        }

        if (shipJSON.getSpeed() != null)
            ship.setSpeed(shipJSON.getSpeed());

        if (shipJSON.getCrewSize() != null) {
            if (shipJSON.getCrewSize() < 1 || shipJSON.getCrewSize() > 9999) throw new BadRequestException();
            ship.setCrewSize(shipJSON.getCrewSize());
        }

        ship.setRating(calcRating(ship));
        return true;
    }
}