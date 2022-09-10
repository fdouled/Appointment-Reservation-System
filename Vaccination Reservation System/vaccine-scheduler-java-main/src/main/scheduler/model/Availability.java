package scheduler.model;


import java.sql.Date;

public class Availability {
   private final Date time;
   private final String username;

   public static class AvailabilityBuilder {
      private final Date time;
      private final String username;
   
      public AvailabilityBuilder(Date time, String username) {
         this.time = time;
         this.username = username;
      }
   
      public Availability build() {

         return new Availability(this);
      }
   }
   
   public Date getTime() {
      return time;
   }

   public String getUsername() {
      return username;
   }

   private Availability(AvailabilityBuilder builder) {
      this.time = builder.time;
      this.username = builder.username;
   }
}