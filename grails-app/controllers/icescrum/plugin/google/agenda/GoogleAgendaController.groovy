package icescrum.plugin.google.agenda

import grails.plugins.springsecurity.Secured

import com.google.gdata.client.calendar.CalendarService
import com.google.gdata.data.DateTime
import com.google.gdata.data.PlainTextConstruct
import com.google.gdata.data.calendar.CalendarEventEntry
import com.google.gdata.data.extensions.When
import com.google.gdata.util.AuthenticationException
import grails.converters.JSON
import org.icescrum.core.domain.Product
import org.icescrum.web.support.MenuBarSupport

@Secured('scrumMaster()')
class GoogleAgendaController {
    static final pluginName = 'ice-scrum-plugin-google-agenda'
    static final id = 'googleAgenda'
    static ui = true
    static menuBar = MenuBarSupport.productDynamicBar('is.ui.googleAgenda',id , false, 3)
    static window =  [title:'is.ui.googleAgenda',help:'is.ui.googleAgenda.help',toolbar:false]

    def index = {
        GoogleCalendarSettings projectAccount = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        if (projectAccount) {
            render template:'displayAccount',
                  plugin:'iceScrum-plugin-google-agenda',
                  model:[id:id,login:projectAccount.login]
        }
        else {
            render template:'setAccount',
                plugin:'iceScrum-plugin-google-agenda',
                model:[id:id]
        }

        getSprints()
    }

    def saveAccount = {
        GoogleCalendarSettings projectAccount = new GoogleCalendarSettings(login:params.googleLogin, password:params.googlePassword, product:Product.get(params.product))
        if(getConnection(params.googleLogin, params.googlePassword)) {
            projectAccount.save()
            redirect(action:'index',params:[product:params.product])
        }
        else
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.error.wrongCredentials')]] as JSON)
    }

    def getConnection(login, password) {
        CalendarService googleService = new CalendarService("test")
        try {
          googleService.setUserCredentials(login, password);
        }
        catch (AuthenticationException e) {
          return false
        }
        return googleService
    }

    def updateCalendar = {
        GoogleCalendarSettings projectAccount = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        CalendarService googleService = getConnection(projectAccount.login, projectAccount.password)
        int i = 1
        // Vider l'agenda !!
        // Ajout des sprints à l'agenda
        getSprints().each {
            createSingleEvent(googleService,
                              "Sprint #" + i++,
                              "no comment",
                              iSDateToGoogleDate(it.startDate),
                              iSDateToGoogleDate(it.endDate))
        }
        // Ajouter les scrum meetings si desirés

        flash.notice = [text: "Okaaaay", type: 'notice']
        redirect(action:'index',params:[product:params.product])
    }

    def getSprints = {
        def sprints = []
        def product = Product.get(params.product);
        product.releases?.each { r->
            r.sprints.asList().each { s->
                 sprints.add(s)
            }
        }
        return sprints.asList()
    }

    def iSDateToGoogleDate (Date date) {
        println("Date version originale : " + date.toString())
        String firstPart = date.toString().substring(0,10)
        String secondPart = date.toString().substring(11,date.toString().indexOf("."))
        if(secondPart.equals("00:00:00")) {
          secondPart = "01:00:00"

        }
        return firstPart + "T" + secondPart
    }

   // Date de format :  Time : "2010-12-31T23:59:59"
   def createSingleEvent(googleService, eventName, comment, startDate, endDate) {
        CalendarEventEntry newEvent = new CalendarEventEntry()
        newEvent.setTitle(new PlainTextConstruct(eventName))
        newEvent.setContent(new PlainTextConstruct(comment))
        When eventTimes = new When()
        eventTimes.setStartTime(DateTime.parseDateTime(startDate))
        eventTimes.setEndTime(DateTime.parseDateTime(endDate))
        newEvent.addTime(eventTimes)

        GoogleCalendarSettings projectAccount = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        URL postUrl = new URL("https://www.google.com/calendar/feeds/"+projectAccount.login+"/private/full")

        CalendarEventEntry insertedEntry = googleService.insert(postUrl, newEvent)
    }
}
