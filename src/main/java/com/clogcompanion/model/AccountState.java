package com.clogcompanion.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/** Immutable snapshot of what the logged-in account has, keyed by constant names. */
@Value
public class AccountState
{
	Map<String, Integer> skillLevels;
	Set<String> completedQuests;
	Set<String> completedDiaries;

	public static AccountState empty()
	{
		return new AccountState(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet());
	}
}
