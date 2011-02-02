package icescrum.plugin.google.agenda

import org.icescrum.core.domain.Product

class GoogleCalendarSettings {

    String login
    String password
    boolean displayDailyMeetings = false
    boolean displayReleasePlanning = false
    boolean displaySprintPlanning = false
    boolean displaySprintReview = false
    boolean displaySprintRetrospective = false

    static constraints = {
      login(nullable:false)
      password(nullable:false)
    }

    static belongsTo = [product:Product]
}