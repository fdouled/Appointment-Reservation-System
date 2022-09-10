package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class Patient {
    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    public static class PatientBuilder {
        private final String username;
        private final byte[] salt;
        private final byte[] hash;

        public PatientBuilder(String username, byte[] salt, byte[] hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }

        public Patient build() {
            return new Patient(this);
        }
    }

    public static class PatientGetter {
        private final String username;
        private final String password;
        private byte[] salt;
        private byte[] hash;

        public PatientGetter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public Patient get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getPatient = "SELECT Salt, Hash FROM Patients WHERE Username = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getPatient);
                statement.setString(1, this.username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    byte[] salt = resultSet.getBytes("Salt");
                    byte[] hash = Util.trim(resultSet.getBytes("Hash"));
                    byte[] calculatedHash = Util.generateHash(password, salt);
                    if (!Arrays.equals(hash, calculatedHash)) {
                        return null;
                    } else {
                        this.salt = salt;
                        this.hash = hash;
                        return new Patient(this);
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private Patient(PatientBuilder builder) {
        this.username = builder.username;
        this.salt = builder.salt;
        this.hash = builder.hash;
    }

    private Patient(PatientGetter getter) {
        this.username = getter.username;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addPatient = "INSERT INTO Patients VALUES (? , ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addPatient);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public String getUsername() {
        return username;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getHash() {
        return hash;
    }

    public List<String> searchAvailability(Date d) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String searchAvailability = "SELECT Username FROM Availabilities WHERE Time = ?";
        List<String> availableCaregiver = new ArrayList<String>();
        try {
            PreparedStatement statement = con.prepareStatement(searchAvailability);
            statement.setDate(1, (java.sql.Date) d);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String caregiver = resultSet.getString("Username");
                availableCaregiver.add(caregiver);
            }
            return availableCaregiver;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void showAvailableVaccine() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String showAvailableVaccine = "SELECT * FROM Vaccines";
        try {
            PreparedStatement statement = con.prepareStatement(showAvailableVaccine);
            ResultSet resultSet = statement.executeQuery();
            int availableVaccines = 0;
            while (resultSet.next()) {
                String vaccineName = resultSet.getString("Vaccine name");
                int availableDoses = resultSet.getInt("Available doses");
                if (availableDoses > 0) {
                    Vaccine vaccine = new Vaccine.VaccineBuilder(vaccineName, availableDoses).build();
                    System.out.println(vaccine);
                    availableVaccines++;
                }
            }
            if (availableVaccines == 0) {
                System.out.println("there are no vaccines available on this day");
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void showAppointment() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String showAppointment = "SELECT * FROM Appointments WHERE PatientName = ?";
        try {
            PreparedStatement statement = con.prepareStatement(showAppointment);
            statement.setString(1, this.username);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("oh no, you have no available appointments under this name");
                return;
            }
            while (resultSet.next()) {
                String appointmentID = resultSet.getString("AppointmentID");
                String vaccine = resultSet.getString("VaccineName");
                Date date = resultSet.getDate("Date");
                String caregiver = resultSet.getString("CaregiverName");
                System.out.println("reservation/appointmentID : " + appointmentID + ", vaccine : " + vaccine +
                        ", date : " + date + ", caregiver : " + caregiver);
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public String reserveAppointment(String caregiver, String vaccine, Date d) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String ID = caregiver.substring(0, 1) + username.substring(0, 1) + vaccine.substring(0, 1) + d.toString();
        String searchAppointment = "SELECT * FROM Appointments WHERE AppointmentID = ?";
        String reserveAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement searchStatement = con.prepareStatement(searchAppointment);
            searchStatement.setString(1, ID);
            ResultSet resultSet = searchStatement.executeQuery();
            if (resultSet.isBeforeFirst()) {
                System.out.println("schedule clash!! there is already a reservation at this time");
                return "";
            }
            PreparedStatement reserveStatement = con.prepareStatement(reserveAppointment);
            reserveStatement.setString(1, caregiver);
            reserveStatement.setString(2, username);
            reserveStatement.setString(3, vaccine);
            reserveStatement.setDate(4, (java.sql.Date) d);
            reserveStatement.setString(5, ID);
            reserveStatement.executeUpdate();
            return ID;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
}
