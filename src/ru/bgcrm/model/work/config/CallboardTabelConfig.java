package ru.bgcrm.model.work.config;

import ru.bgcrm.util.Config;
import ru.bgcrm.util.ParameterMap;

public class CallboardTabelConfig
	extends Config
{
	private final String templatePath;
	private final int paramTabelNumber;
	private final int paramDolznost;
	private final String orgName;
	
	public CallboardTabelConfig( ParameterMap setup )
	{
		super( setup );
		
		this.templatePath = setup.get( "template" );
		this.paramTabelNumber = setup.getInt( "paramTabelNumberId", 0 );
		this.paramDolznost = setup.getInt( "paramPostId", 0 );
		this.orgName = setup.get( "departmentTitle", "" );
	}

	public String getTemplatePath()
	{
		return templatePath;
	}

	public int getParamTabelNumber()
	{
		return paramTabelNumber;
	}

	public int getParamDolznost()
	{
		return paramDolznost;
	}

	public String getOrgName()
	{
		return orgName;
	}
}
