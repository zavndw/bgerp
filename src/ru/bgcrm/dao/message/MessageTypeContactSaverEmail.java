package ru.bgcrm.dao.message;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import org.apache.commons.lang.StringUtils;

import ru.bgcrm.dao.ParamValueDAO;
import ru.bgcrm.dao.process.ProcessLinkDAO;
import ru.bgcrm.model.BGException;
import ru.bgcrm.model.BGMessageException;
import ru.bgcrm.model.CommonObjectLink;
import ru.bgcrm.model.IdTitle;
import ru.bgcrm.model.customer.Customer;
import ru.bgcrm.model.message.Message;
import ru.bgcrm.model.param.ParameterEmailValue;
import ru.bgcrm.model.process.Process;
import ru.bgcrm.struts.form.DynActionForm;
import ru.bgcrm.util.ParameterMap;
import ru.bgcrm.util.Utils;

public class MessageTypeContactSaverEmail extends MessageTypeContactSaver {
    private static final List<IdTitle> MODE_LIST = new ArrayList<IdTitle>();
    static {
        MODE_LIST.add(new IdTitle(0, "Не сохранять"));
        MODE_LIST.add(new IdTitle(1, "Cохранить EMail"));
        MODE_LIST.add(new IdTitle(2, "Cохранить EMail домен"));
    }

    private int paramId;

    public MessageTypeContactSaverEmail(ParameterMap config) throws BGException {
        super(config);

        this.paramId = config.getInt("paramId", -1);
        if (paramId <= 0) {
            throw new BGMessageException("paramId not defined!");
        }
    }

    @Override
    public List<IdTitle> getSaveModeList() {
        return MODE_LIST;
    }

    @Override
    public void saveContact(DynActionForm form, Connection con, Message message, Process process, int saveMode) throws BGException {
        CommonObjectLink customerLink = Utils.getFirst(new ProcessLinkDAO(con).getObjectLinksWithType(process.getId(), Customer.OBJECT_TYPE));
        if (customerLink == null) {
            return;
        }

        String email = message.getFrom();

        String domainName = StringUtils.substringAfter(email, "@");

        ParamValueDAO paramDao = new ParamValueDAO(con);

        SortedMap<Integer, ParameterEmailValue> currentValue = paramDao.getParamEmail(customerLink.getLinkedObjectId(), paramId);

        boolean exists = false;
        for (ParameterEmailValue value : currentValue.values()) {
            if (exists = value.getValue().equals(email) || value.getValue().equals(domainName)) {
                break;
            }
        }

        if (!exists) {
            paramDao.updateParamEmail(customerLink.getLinkedObjectId(), paramId, 0, new ParameterEmailValue(saveMode == 1 ? email : domainName));
        }
    }
}
