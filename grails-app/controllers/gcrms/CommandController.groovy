package gcrms

class CommandController {

    def searchService
    def index() { }

    /**
     *
     * @param param: search_data "YYYY-MM-DD"
     * @return
     */
    def diff() {
        searchService.dataDiff(params)
    }
}
