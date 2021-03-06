package ru.bgcrm.model.work.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.bgcrm.model.work.Shortcut;
import ru.bgcrm.util.Config;
import ru.bgcrm.util.ParameterMap;

public class ShortcutConfig
	extends Config
{
	private final Map<Integer, Shortcut> shortcutMap = new LinkedHashMap<Integer, Shortcut>();
	
	public ShortcutConfig( ParameterMap setup )
	{
		super( setup );
		
		for( Map.Entry<Integer, ParameterMap>  me : setup.subIndexed( "callboard.workdays.shortcut." ).entrySet() )
		{
			int id = me.getKey();
			ParameterMap config = me.getValue();
			
			shortcutMap.put( id, new Shortcut( id, config.get( "title", "" ), config.get( "value", "" ) ) );
		}
	}
	
	public Map<Integer, Shortcut> getShortcutMap()
	{
		return shortcutMap;
	}

	public Collection<Shortcut> getShortcuts()
	{
		return shortcutMap.values();
	}
	
	public Shortcut getShortcut( int id )
	{
		return shortcutMap.get( id );
	}
	
}
