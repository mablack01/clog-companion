package com.clogcompanion.engine;

import com.clogcompanion.model.Category;
import com.clogcompanion.model.Tier;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/** Mutable filter criteria; the panel binds its controls to one instance. */
@Getter
@Setter
public class ClogFilter
{
	private Set<Tier> tiers = EnumSet.allOf(Tier.class);
	private Category category;
	private boolean hideObtained = true;
	private boolean onlyMeetsRequirements;
	private String search = "";

	public boolean test(RatedSlot s)
	{
		if (hideObtained && s.isObtained())
		{
			return false;
		}
		if (!tiers.contains(s.getTier()))
		{
			return false;
		}
		if (category != null && s.getSource().getCategory() != category)
		{
			return false;
		}
		if (onlyMeetsRequirements && !s.getUnmet().isEmpty())
		{
			return false;
		}
		String q = search.trim().toLowerCase(Locale.ROOT);
		return q.isEmpty()
			|| s.getItem().getName().toLowerCase(Locale.ROOT).contains(q)
			|| s.getSource().getName().toLowerCase(Locale.ROOT).contains(q);
	}
}
