package gcrms

import grails.databinding.BindingFormat

class BooksScanTimeOld {
    Date createTime
    Date scanStartTime
    Date scanEndTime
    @BindingFormat("yyyy-MM-dd")
    java.sql.Date curDate
    boolean compared

    static constraints = {
    }

    static mapping = {
        version false
    }
}
