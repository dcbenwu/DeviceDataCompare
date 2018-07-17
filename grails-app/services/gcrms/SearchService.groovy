package gcrms

import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.locks.ReentrantLock

//@Transactional
class SearchService {

    //def dataSource_sec
    def lock = new ReentrantLock()

    def goStateful() {
        if (lock.tryLock()) {
            try {
                Thread.sleep(300)
                println("Take the job " + Thread.currentThread().name )
                look()
                println("The job " + Thread.currentThread().name + " is done.")
            } catch (Exception e) {
                e.printStackTrace()
                log.info(e.getMessage())
            } finally {
                lock.unlock()
                log.info("Release lock")
            }
        } else {
            println("waiting previous job done...")
        }

    }

    // check scan table to see if any new
    def look() {
        def newDate
        // find last not compared date
        def lastDate //= BooksEqpt.last().search_date
        def lastList = BooksEqpt.findAll(sort: 'id', order: 'asc', max: 10) {state == null || state  == ''}
        if (lastList.size() > 0) {
            lastDate = lastList.get(0).search_date
            log.info("Find previous not compared date id " + lastList.get(0).id + " date: " + lastDate )
        } else {
            log.info("Not found previous not compared books eqpt data. Exiting ...")
            return
        }


        def newArrivalList = BooksScanTime.findAll(sort: 'id', order: "asc", max: 10) {curDate >= lastDate && compared == false}
        def newArrival
        if (newArrivalList.size() >0 ){
            log.info("-----find new newArrivalList-----" )
            newArrivalList.each {
                log.info("current date:" + it.curDate)
            }
            log.info("-----total: " + newArrivalList.size() + "-----")
            newArrival = newArrivalList.get(0)
        } else {
            log.info(" not found new arrival date. Exiting ...")
            return
        }

        //newArrival = newArrivalList.empty? null:newArrivalList.first()
        if (newArrival) {
            newDate = newArrival.curDate
            log.info("new data arrive " + newDate)
            // set to be compared
            newArrival.compared = true
            newArrival.save()

            compareInfo(newDate)
        }
    }

    def compareInfo(newDateIn) {

        def currentDate
        def now = new Date()
        def yesterday = now -1
        if(newDateIn) {
            currentDate = newDateIn
        } else {
            currentDate = now.format("yyyy-MM-dd")
        }

        def lastDate
        // found the previous date
        def preDateList = BooksEqpt.findAll(sort: "id", order: "desc", max: 10){search_date < currentDate}
        if (preDateList.size() == 10) {
            if (preDateList[0].search_date == preDateList[1].search_date) {
                lastDate = preDateList[0].search_date
            } else if (preDateList[1].search_date == preDateList[2].search_date) {
                lastDate = preDateList[1].search_date
            } else if (preDateList[2].search_date == preDateList[3].search_date) {
                lastDate = preDateList[2].search_date
            } else {
                log.error("Something should be wrong. Exiting ...")
                return 1
            }
        }

        log.info("Debug: lastDate is " + lastDate)
        log.info("Debug: currentDate is " + currentDate)


        def eqpt_list_current = BooksEqpt.findAll {search_date == currentDate}
        log.info("today's scaned devices amount: " + eqpt_list_current.size())
        def eqpt_list_last = BooksEqpt.findAll{search_date == lastDate && state != "delete" && state != "move-"}
        log.info("previous day scaned devices amount: " + eqpt_list_last.size())
        def add_or_move = []
        eqpt_list_current.each { curOne -> // new device
            eqpt_list_last.each { preOne -> // previous device

            }
            def preDeviceList = eqpt_list_last.findAll { preOne -> preOne.ip == curOne.ip }
            if (preDeviceList.size() > 0) {
                /*preDeviceList.each { preDevice ->
                    if (preDevice.sn == curOne.sn && preDevice.pn == curOne.pn) {
                        if (preDevice.state == 'delete') {
                            curOne.state = 'add'
                            curOne.save()
                        } else {
                            curOne.state = 'normal'
                            curOne.save()
                        }
                        // remove the find one from previous list
                        eqpt_list_last.remove(preDevice)
                        return
                    }
                }*/

                def findPreDevice = preDeviceList.find{preDevice -> preDevice.sn == curOne.sn && preDevice.pn == curOne.pn}
                if (findPreDevice) {
                    if (findPreDevice.state == 'delete') {
                        curOne.state = 'add'
                        curOne.save()
                    } else {
                        curOne.state = 'normal'
                        curOne.save()
                    }
                    // remove the find one from previous list
                    eqpt_list_last.remove(findPreDevice)
                } else {
                    log.info("not found the device sn,pn " + curOne.sn + ", " + curOne.pn + " in the address " + curOne.ip)
                    add_or_move.add(curOne)
                }
            } else {
                log.info("not found the same ip device sn,pn,ip " + curOne.sn + ", " + curOne.pn + ", " + curOne.ip)
                add_or_move.add(curOne)
            }
        }

        add_or_move.each { add_or_move_one ->
            def find_sn_pn = eqpt_list_last.find{preOne -> preOne.sn == add_or_move_one.sn && preOne.pn == add_or_move_one.pn}
            if (find_sn_pn) {
                log.info("find moved device, sn: " + find_sn_pn.sn + ", pn: " + find_sn_pn.pn)
                add_or_move_one.state = 'move+'
                add_or_move_one.save()
                // then, plus a move- record
                new BooksEqpt(ip: find_sn_pn.ip,
                        device_type: 'product',
                        type: find_sn_pn.type,
                        sn: find_sn_pn.sn,
                        pn: find_sn_pn.pn,
                        location: find_sn_pn.location,
                        search_date: currentDate,
                        state: 'move-'
                ).save()
                // remove the find one from previous list
                eqpt_list_last.remove(find_sn_pn)
            } else {
                // should be a new add one
                add_or_move_one.state = 'add'
                add_or_move_one.save()
            }
        }

        // then, the lest is delete ones
        log.info("the amount of delete state devices is " + eqpt_list_last.size())
        eqpt_list_last.each { one ->
            new BooksEqpt(ip: one.ip,
                    device_type: 'product',
                    type: one.type,
                    sn: one.sn,
                    pn: one.pn,
                    location: one.location,
                    search_date: currentDate,
                    state: 'delete'
            ).save()
        }


    }

    /*def dataDiff(params) {
        def listA = BooksEqpt.findAll(sort: 'id', order: 'asc'){search_date == params.search_date}
        def sql = "select * from books_eqpt where search_date = :search_date order by id asc"
        def db = new groovy.sql.Sql(dataSource: dataSource_sec)
        //def listB = BooksEqpt.sec.findAll(sort: 'id', order: 'asc'){search_date == params.search_date}
        def listB = db.rows(sql,search_date: params.search_date)
        println("done")
    }
*/
    /**
     * print " in compare_info process ..."
     e_ip = 0
     add_or_move = []
     eqpt_list_current = EQPT.objects.filter(search_date = self.current_date)
     eqpt_list_last = EQPT.objects.filter(search_date = self.last_date).order_by('id').exclude(state='delete').exclude(state='move-')
     current_len = len(eqpt_list_current)
     last_len = len(eqpt_list_last)
     print "current length is ",current_len," last day length is ",last_len
     print time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
     for i in range(0,current_len):
     k=0
     for j in range(0,last_len):
     e_ip = 0
     if eqpt_list_current[i].ip == eqpt_list_last[j].ip:
     e_ip = 1
     if eqpt_list_current[i].sn == eqpt_list_last[j].sn and eqpt_list_current[i].pn == eqpt_list_last[j].pn:
     k=1
     if eqpt_list_last[j].state == 'delete':
     eqpt_list_current[i].state='add'
     eqpt_list_current[i].save()
     else:
     eqpt_list_current[i].state='normal'
     eqpt_list_current[i].save()
     eqpt_list_last = eqpt_list_last.exclude(id=eqpt_list_last[j].id)
     break
     elif e_ip == 1:
     break
     if k==0:
     add_or_move.append(eqpt_list_current[i])
     print time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
     for i in add_or_move:
     k=0
     for j in eqpt_list_last:
     if i.sn == j.sn and i.pn == j.pn:
     k=1
     i.state='move+'
     i.save()
     eqpt = EQPT(ip=j.ip,device_type='product',type=j.type,pn=j.pn,sn=j.sn,
     location=j.location,search_date=self.current_date,
     state='move-')
     eqpt.save()
     eqpt_list_last = eqpt_list_last.exclude(id=j.id)
     break
     if k==0:
     i.state='add'
     i.save()
     print time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
     for j in eqpt_list_last:
     eqpt = EQPT(ip=j.ip,device_type='product',type=j.type,pn=j.pn,sn=j.sn,
     location=j.location.strip(),search_date=self.current_date,
     state='delete')
     eqpt.save()
     */
}
