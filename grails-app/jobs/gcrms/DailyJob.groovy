package gcrms

class DailyJob {

    def searchService

    static triggers = {
        //cron name: 'DailyJob', cronExpression: "0 0 4 * * ? *" // seconds, minutes, hours,
        simple repeatInterval: 2*60*1000l, startDelay: 10*1000l // execute job once in  seconds * 60
    }

    def execute() {
        // execute job
        searchService.goStateful()
    }
}
