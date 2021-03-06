package ru.bgcrm.struts.action;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.SerializationUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import ru.bgcrm.cache.ProcessQueueCache;
import ru.bgcrm.cache.ProcessTypeCache;
import ru.bgcrm.dao.process.ProcessDAO;
import ru.bgcrm.dao.process.SavedFilterDAO;
import ru.bgcrm.dao.user.UserDAO;
import ru.bgcrm.event.EventProcessor;
import ru.bgcrm.event.ProcessMarkedActionEvent;
import ru.bgcrm.event.process.QueuePrintEvent;
import ru.bgcrm.model.ArrayHashMap;
import ru.bgcrm.model.BGIllegalArgumentException;
import ru.bgcrm.model.BGMessageException;
import ru.bgcrm.model.Page;
import ru.bgcrm.model.SearchResult;
import ru.bgcrm.model.process.Process;
import ru.bgcrm.model.process.Queue;
import ru.bgcrm.model.process.Queue.ColumnConf;
import ru.bgcrm.model.process.queue.Processor;
import ru.bgcrm.model.process.queue.config.SavedCommonFiltersConfig;
import ru.bgcrm.model.process.queue.config.SavedFilter;
import ru.bgcrm.model.process.queue.config.SavedFiltersConfig;
import ru.bgcrm.model.process.queue.config.SavedFiltersConfig.SavedFilterSet;
import ru.bgcrm.model.process.queue.config.SavedPanelConfig;
import ru.bgcrm.model.user.User;
import ru.bgcrm.plugin.report.model.PrintQueueConfig;
import ru.bgcrm.plugin.report.model.PrintQueueConfig.PrintType;
import ru.bgcrm.struts.form.DynActionForm;
import ru.bgcrm.util.Preferences;
import ru.bgcrm.util.Utils;
import ru.bgcrm.util.sql.ConnectionSet;
import ru.bgerp.util.Log;

public class ProcessQueueAction extends ProcessAction {
    private static final Log log = Log.getLog();
    
    // выбранные в полном фильтре фильтры
    private static final String QUEUE_FULL_FILTER_SELECTED_FILTERS = "queueSelectedFilters";
    // параметры полного фильтра
    private static final String QUEUE_FULL_FILTER_PARAMS = "queueCurrentSavedFiltersParam.";
    
    public ActionForward queue(ActionMapping mapping, DynActionForm form, ConnectionSet conSet) throws Exception {
        form.getResponse().setData("list", ProcessQueueCache.getUserQueueList(form.getUser()));

        return data(conSet, mapping, form, "queue");
    }
    
    // возвращает дерево типов для создания процесса
    public ActionForward typeTree(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        int queueId = Utils.parseInt(form.getParam("queueId"));
        Queue queue = ProcessQueueCache.getQueue(queueId, form.getUser());

        // очередь не разрешена пользователю
        if (queue == null)
            return data(con, mapping, form, "processTypeTree");

        var typeList = ProcessTypeCache.getTypeList(queue.getProcessTypeIds());
        applyProcessTypePermission(typeList, form);
        form.getHttpRequest().setAttribute("typeTreeRoot", ProcessTypeCache.getTypeTreeRoot().sub(typeList));

        return data(con, mapping, form, "processTypeTree");
    }
    
    public ActionForward processCustomClassInvoke(ActionMapping mapping, DynActionForm form, ConnectionSet conSet) throws Exception {
        Queue queue = ProcessQueueCache.getQueue(form.getParamInt("queueId"), form.getUser());
        if (queue != null) {
            Processor processor = queue.getProcessorMap().get(form.getParamInt("processorId"));
            List<Integer> processIds = Utils.toIntegerList(form.getParam("processIds"));

            ProcessMarkedActionEvent event = new ProcessMarkedActionEvent(form, processor, processIds);
            EventProcessor.processEvent(event, processor.getClassName(), conSet);

            if (event.isStreamResponse()) {
                return null;
            } else {
                return status(conSet, form);
            }
        }

        return data(conSet, mapping, form, FORWARD_DEFAULT);
    }

    public ActionForward queueSavedFilterSet(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        Preferences personalizationMap = form.getUser().getPersonalizationMap();
        SavedFiltersConfig config = personalizationMap.getConfig(SavedFiltersConfig.class);

        String persConfigBefore = personalizationMap.getDataString();

        String command = form.getParam("command");
        int queueId = form.getParamInt("queueId");
        if (Utils.isBlankString(command) || queueId <= 0) {
            throw new BGIllegalArgumentException();
        }

        ArrayList<SavedFilter> commonFilters = new SavedFilterDAO(con).getFilters(queueId);
        SavedCommonFiltersConfig commonConfig = new SavedCommonFiltersConfig(commonFilters);

        if (command.equals("delete")) {
            config.removeSavedFilterSet(queueId, form.getId());
            personalizationMap.removeSub(SavedFiltersConfig.QUEUE_CURRENT_SAVED_FILTER_SET_PREFIX + queueId);
        } else if (command.equals("add")) {
            String title = form.getParam("title");
            String url = form.getParam("url");

            if (Utils.isBlankString(title) || Utils.isBlankString(url)) {
                throw new BGIllegalArgumentException();
            }

            int createdSetId = config.addSavedFilterSet(queueId, title, url);

            personalizationMap.put(SavedFiltersConfig.QUEUE_CURRENT_SAVED_FILTER_SET_PREFIX + queueId, String.valueOf(createdSetId));
        } else if (command.equals("select")) {
            if (form.getId() < 0) {
                throw new BGIllegalArgumentException();
            }
            personalizationMap.put(SavedFiltersConfig.QUEUE_CURRENT_SAVED_FILTER_SET_PREFIX + queueId, String.valueOf(form.getId()));
        } else if (command.equals("toFullFilter")) {
            // extracting from unexisting empty filter - reset full filter
            SavedFilterSet filter = config.getSavedFilterSetMap().get(form.getId());
            DynActionForm savedFiltersForm = filter != null ? new DynActionForm(filter.getUrl()) : new DynActionForm();
            saveFormFilters(queueId, savedFiltersForm, personalizationMap);
            personalizationMap.remove(QUEUE_FULL_FILTER_SELECTED_FILTERS + queueId);
        } else if (command.equals("updateFiltersOrder")) {
            config.reorderSavedFilterSets(queueId, form.getSelectedValuesList("setId"));
        } else if (command.equals("setRareStatus")) {
            int filterId = form.getParamInt("filterId");
            Boolean value = form.getParamBoolean("rare", false);

            log.debug("set rare status: " + value + " " + filterId);
            config.setRareStatus(queueId, filterId, value);
        } else if (command.equals("setStatusCounterOnPanel")) {
            int filterId = form.getParamInt("filterId");
            Boolean value = form.getParamBoolean("statusCounterOnPanel", false);
            String color = form.getParam("color");
            String title = form.getParam("title");
            String queueName = form.getParam("queueName");
            log.debug("set counter on panel status: " + value + " " + filterId);
            config.setStatusCounterOnPanel(queueId, filterId, color, value, title, queueName);
        } else if (command.equals("addCommon")) {
            String title = form.getParam("title");
            String url = form.getParam("url");
            log.debug("Adding common filter " + title + " " + url);
            if (Utils.isBlankString(title) || Utils.isBlankString(url)) {
                throw new BGIllegalArgumentException();
            }
            commonConfig.addSavedCommonFilter(queueId, title, url);

            new SavedFilterDAO(con).updateFilter(commonConfig, queueId);
        } else if (command.equals("importCommon")) {
            String title = form.getParam("title");
            int id = form.getParamInt("id");
            String url = new SavedFilterDAO(con).getFilterUrlById(id);

            log.debug("Importing common filter " + title + " " + url);

            if (Utils.isBlankString(title) || Utils.isBlankString(url)) {
                throw new BGIllegalArgumentException();
            }
            int createdSetId = config.addSavedFilterSet(queueId, title, url);

            personalizationMap.put(SavedFiltersConfig.QUEUE_CURRENT_SAVED_FILTER_SET_PREFIX + queueId, String.valueOf(createdSetId));
        } else if (command.equals("deleteCommon")) {
            String title = form.getParam("title");
            int id = form.getParamInt("id");
            String url = new SavedFilterDAO(con).getFilterUrlById(id);

            log.debug("Deleting common filter " + title + " " + url);

            if (Utils.isBlankString(title) || Utils.isBlankString(url)) {
                throw new BGIllegalArgumentException();
            }
            commonConfig.deleteSavedCommonFilter(queueId, title, url);

            new SavedFilterDAO(con).deleteFilter(id);
        }

        config.updateConfig(personalizationMap);

        new UserDAO(con).updatePersonalization(persConfigBefore, form.getUser());

        return status(con, form);
    }

    public ActionForward queueSavedPanelSet(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        Preferences personalizationMap = form.getUser().getPersonalizationMap();
        SavedPanelConfig config = personalizationMap.getConfig(SavedPanelConfig.class);

        String persConfigBefore = personalizationMap.getDataString();

        String command = form.getParam("command");
        Integer queueId = form.getParamInt("queueId");
        log.debug(command + " " + queueId);

        if (Utils.isBlankString(command) || queueId <= 0) {
            throw new BGIllegalArgumentException();
        }

        if (command.equals("add")) {
            config.addSavedPanelSet(queueId);
        } else if (command.equals("delete")) {
            config.removeSavedPanelSet(queueId);
        } else if (command.equals("updateSelected")) {
            config.changeCurrentSelected(queueId);
        }

        config.updateConfig(personalizationMap);
        new UserDAO(con).updatePersonalization(persConfigBefore, form.getUser());

        return status(con, form);

    }

    public ActionForward queueGet(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        User user = form.getUser();
        HttpServletRequest request = form.getHttpRequest();

        Queue queue = ProcessQueueCache.getQueue(form.getId(), user);
        if (queue != null && form.getUser().getQueueIds().contains(queue.getId())) {
            ArrayList<SavedFilter> commonFilters = new SavedFilterDAO(con).getFilters(queue.getId());
            SavedCommonFiltersConfig commonConfig = new SavedCommonFiltersConfig(commonFilters);
            request.setAttribute("commonConfig", commonConfig);

            form.getResponse().setData("queue", queue);
            // form.getResponse().setData("statusList", new StatusDAO(con).getStatusList());

            var typeList = ProcessTypeCache.getTypeList(queue.getProcessTypeIds());
            applyProcessTypePermission(typeList, form);
            form.getResponse().setData("typeList", typeList);

            Preferences personalizationMap = user.getPersonalizationMap();
            String persConfigBefore = personalizationMap.getDataString();

            personalizationMap.put("queueLastSelected", String.valueOf(queue.getId()));

            String filtersValues = personalizationMap.get(QUEUE_FULL_FILTER_PARAMS + form.getId());
            if (!Utils.isEmptyString(filtersValues) || filtersValues != null) {
                ArrayHashMap ahm = (ArrayHashMap) SerializationUtils.deserialize(Base64.getDecoder().decode(filtersValues));
                DynActionForm savedParamsFilters = new DynActionForm();
                savedParamsFilters.setParam(ahm);
                request.setAttribute("savedParamsFilters", savedParamsFilters);
            }

            new UserDAO(con).updatePersonalization(persConfigBefore, user);
        }

        return data(con, mapping, form, "queueFilter");
    }

    public ActionForward queueShow(ActionMapping mapping, DynActionForm form, ConnectionSet connectionSet) throws Exception {
        Preferences personalizationMap = form.getUser().getPersonalizationMap();

        String configBefore = personalizationMap.getDataString();

        int savedFilterSetId = form.getParamInt("savedFilterSetId");
        // выбранные в полном фильтре фильтры
        String selectedFilters = form.getParam("selectedFilters");

        personalizationMap.put(SavedFiltersConfig.QUEUE_CURRENT_SAVED_FILTER_SET_PREFIX + form.getId(), String.valueOf(savedFilterSetId));
        if (selectedFilters != null) {
            personalizationMap.put(QUEUE_FULL_FILTER_SELECTED_FILTERS + form.getId(), selectedFilters);
        }

        // полный фильтр - сохранение параметров запроса
        if (savedFilterSetId == 0) {
            //TODO: Сохранение параметров стоит сделать пробегая непосредственно по фильтрам
            //и сортировкам, это исключит различные посторонние параметры вроде requestUrl.
            saveFormFilters(form.getId(), form, personalizationMap);
        }

        // параметры изменились
        new UserDAO(connectionSet.getConnection()).updatePersonalization(configBefore, form.getUser());

        if (!form.getUser().getQueueIds().contains(form.getId())) {
            throw new BGMessageException("Вам не разрешён доступ к очереди процессов с ID=%s!", form.getId());
        }

        Queue queue = ProcessQueueCache.getQueue(form.getId(), form.getUser());
        if (queue != null) {
            SearchResult<Object[]> searchResult = new SearchResult<Object[]>(form);
            List<String> aggregateValues = new ArrayList<>();

            var media = getMedia(form);
            var noHtmlMedia = !Queue.MEDIA_HTML.equals(media);
            if (noHtmlMedia)
                searchResult.getPage().setPageIndex(Page.PAGE_INDEX_NO_PAGING);

            ProcessDAO processDAO = new ProcessDAO(connectionSet.getSlaveConnection(), form.getUser());
            processDAO.searchProcess(searchResult, aggregateValues, queue, form);

            final List<Object[]> list = searchResult.getList();

            if (noHtmlMedia) {
                processNoHtmlResult(media, form, queue, connectionSet, list);
                return null;
            }

            HttpServletRequest request = form.getHttpRequest();
            request.setAttribute("columnList", queue.getMediaColumnList(Queue.MEDIA_HTML));
            queue.processDataForMedia(form, Queue.MEDIA_HTML, list);
            request.setAttribute("queue", queue);
            if (aggregateValues.size() > 0)
                form.setResponseData("aggregateValues", aggregateValues);
        } else {
            throw new BGMessageException("Очередь процессов с ID=%s не найдена!", form.getId());
        }

        return data(connectionSet, mapping, form);
    }

    private void saveFormFilters(int queueId, DynActionForm form, Preferences personalizationMap) {
        ArrayHashMap ahm = new ArrayHashMap();
        for (String paramName : form.getParam().keySet()) {
            if (!paramName.equals("requestUrl") && form.getParamArray(paramName) != null && form.getParamArray(paramName).length > 0) {
                if (form.getParamArray(paramName).length > 1) {
                    ahm.put(paramName, form.getParamArray(paramName));
                } else if (Utils.notBlankString(form.getParam(paramName))) {
                    ahm.put(paramName, form.getParam(paramName));
                }
            }
        }

        String paramKey = QUEUE_FULL_FILTER_PARAMS + queueId;
        personalizationMap.put(paramKey, Base64.getEncoder().encodeToString(SerializationUtils.serialize(ahm)));
    }

    private String getMedia(DynActionForm form) {
        if (Utils.notBlankString(form.getParam("print")))
            return Queue.MEDIA_PRINT;
        if (Utils.notBlankString(form.getParam("xls")))
            return Queue.MEDIA_XLS;
        return Queue.MEDIA_HTML;
    }

    private void processNoHtmlResult(String media, DynActionForm form, Queue queue, ConnectionSet connectionSet, List<Object[]> list) throws Exception {
        // печать только выбранных
        Set<Integer> processIds = Utils.toIntegerSet(form.getParam("processIds"));
        if (processIds.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                Process process = ((Process[]) list.get(i)[0])[0];

                if (!processIds.contains(process.getId())) {
                    list.remove(i--);
                }
            }
        }

        if (Queue.MEDIA_PRINT.equals(media)) {
            int printTypeId = form.getParamInt("printTypeId");
            if (printTypeId > 0) {
                PrintQueueConfig config = queue.getConfigMap().getConfig(PrintQueueConfig.class);
                PrintType printType = config.getPrintType(printTypeId);

                queue.processDataForColumns(form, list, queue.getColumnConfList(printType.getColumnIds()), false);
                EventProcessor.processEvent(new QueuePrintEvent(form, list, queue, printType), "", connectionSet);
            } else {
                // TODO: В метод необходимо вынести расшифровку всех справочников.
                // FIXME: В данный момент это событие обрабатывает модуль отчётов, наверное нужно запретить печать, если этот модуль не установлен.
                queue.processDataForMedia(form, media, list);
                EventProcessor.processEvent(new QueuePrintEvent(form, list, queue, null), "", connectionSet);
            }
        } else if (Queue.MEDIA_XLS.equals(media)) {
            queue.processDataForMedia(form, media, list);

            try (HSSFWorkbook workbook = new HSSFWorkbook()) {
                HSSFSheet sheet = workbook.createSheet("BGERP processes");

                List<ColumnConf> columnList = queue.getMediaColumnList(media);

                Row titleRow = sheet.createRow(0);

                for (int i = 0; i < columnList.size(); i++) {
                    Cell titleCell = titleRow.createCell(i);
                    titleCell.setCellValue(columnList.get(i).getTitle());
                }

                for (int k = 0; k < list.size(); k++) {
                    //Create a new row in current sheet
                    Row row = sheet.createRow(k + 1);
                    Object[] dataRow = list.get(k);

                    for (int i = 0; i < dataRow.length; i++) {
                        if (dataRow[i].equals("null")) {
                            continue;
                        } else {
                            //Create a new cell in current row
                            Cell cell = row.createCell(i);
                            cell.setCellValue(dataRow[i].toString());
                        }
                    }
                }

                HttpServletResponse response = form.getHttpResponse();
                response.setContentType("application/vnd.ms-excel");
                response.setHeader("Content-Disposition", "attachment; filename=bgerp_process_list.xls");
                workbook.write(response.getOutputStream());
            }
        }
    }
}