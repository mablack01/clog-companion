package com.clogcompanion.ui;

import com.clogcompanion.engine.ClogFilter;
import com.clogcompanion.engine.ClogSorter;
import com.clogcompanion.engine.RatedSlot;
import com.clogcompanion.model.Category;
import com.clogcompanion.model.Tier;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

/** Sidebar: filter bar over a paged list of rated slots. All methods run on the EDT. */
public class ClogPanel extends PluginPanel
{
	private static final int PAGE = 50;

	private final ItemManager itemManager;
	private final ClogFilter filter;
	private final Runnable onFilterPersist;
	private final JLabel status = new JLabel();
	private final JLabel count = new JLabel();
	private final JPanel list = new JPanel();
	private final JButton more = new JButton("Show 50 more");
	private final JComboBox<ClogSorter> sort = new JComboBox<>(ClogSorter.values());
	private List<RatedSlot> slots = Collections.emptyList();
	private List<RatedSlot> visible = Collections.emptyList();
	private int shown;

	public ClogPanel(ItemManager itemManager, ClogFilter filter, Runnable onFilterPersist)
	{
		super(false);
		this.itemManager = itemManager;
		this.filter = filter;
		this.onFilterPersist = onFilterPersist;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Clog Companion");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		top.add(title);
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		top.add(status);
		top.add(Box.createVerticalStrut(6));

		JPanel tiers = new JPanel(new GridLayout(1, 0, 2, 0));
		tiers.setOpaque(false);
		for (Tier tier : Tier.values())
		{
			JToggleButton b = new JToggleButton(tier == Tier.UNRATED ? "?" : tier.name().substring(0, 1), true);
			b.setToolTipText(tier.name());
			b.setForeground(ClogItemRow.tierColor(tier));
			b.setFocusPainted(false);
			b.addActionListener(e ->
			{
				EnumSet<Tier> set = EnumSet.copyOf(filter.getTiers());
				if (b.isSelected())
				{
					set.add(tier);
				}
				else
				{
					set.remove(tier);
				}
				filter.setTiers(set);
				apply();
			});
			tiers.add(b);
		}
		top.add(tiers);
		top.add(Box.createVerticalStrut(4));

		JComboBox<Category> category = new JComboBox<>();
		category.addItem(null);
		for (Category c : Category.values())
		{
			category.addItem(c);
		}
		category.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f)
			{
				return super.getListCellRendererComponent(l, v == null ? "All categories" : titleCase(v.toString()), i, s, f);
			}
		});
		category.addActionListener(e ->
		{
			filter.setCategory((Category) category.getSelectedItem());
			apply();
		});
		top.add(category);
		top.add(Box.createVerticalStrut(4));

		sort.addActionListener(e -> apply());
		top.add(sort);
		top.add(Box.createVerticalStrut(4));

		JCheckBox hideObtained = check("Hide obtained", filter.isHideObtained());
		hideObtained.addActionListener(e ->
		{
			filter.setHideObtained(hideObtained.isSelected());
			persistAndApply();
		});
		JCheckBox meetsReqs = check("Only what I can do", filter.isOnlyMeetsRequirements());
		meetsReqs.addActionListener(e ->
		{
			filter.setOnlyMeetsRequirements(meetsReqs.isSelected());
			persistAndApply();
		});
		top.add(hideObtained);
		top.add(meetsReqs);

		IconTextField search = new IconTextField();
		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 30));
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				changed();
			}

			private void changed()
			{
				filter.setSearch(search.getText());
				apply();
			}
		});
		top.add(search);
		top.add(Box.createVerticalStrut(4));
		count.setFont(FontManager.getRunescapeSmallFont());
		count.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		top.add(count);
		add(top, BorderLayout.NORTH);

		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		list.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
		add(list, BorderLayout.CENTER);

		more.setFocusPainted(false);
		more.addActionListener(e ->
		{
			shown += PAGE;
			render();
		});
	}

	public void setSlots(List<RatedSlot> slots)
	{
		this.slots = slots;
		apply();
	}

	public void setStatus(String text)
	{
		status.setText(text);
	}

	private void persistAndApply()
	{
		onFilterPersist.run();
		apply();
	}

	private void apply()
	{
		visible = slots.stream().filter(filter::test).sorted((ClogSorter) sort.getSelectedItem()).collect(Collectors.toList());
		shown = PAGE;
		render();
	}

	private void render()
	{
		list.removeAll();
		count.setText(visible.size() + " of " + slots.size() + " slots");
		List<RatedSlot> page = new ArrayList<>(visible.subList(0, Math.min(shown, visible.size())));
		for (RatedSlot slot : page)
		{
			list.add(new ClogItemRow(slot, itemManager));
			list.add(Box.createVerticalStrut(3));
		}
		if (visible.size() > shown)
		{
			list.add(more);
		}
		list.revalidate();
		list.repaint();
	}

	private static JCheckBox check(String text, boolean selected)
	{
		JCheckBox box = new JCheckBox(text, selected);
		box.setOpaque(false);
		box.setForeground(ColorScheme.TEXT_COLOR);
		box.setFont(FontManager.getRunescapeSmallFont());
		return box;
	}

	private static String titleCase(String constant)
	{
		return constant.charAt(0) + constant.substring(1).toLowerCase();
	}
}
