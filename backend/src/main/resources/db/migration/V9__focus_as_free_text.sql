-- Focus is no longer a reusable catalog: it's free text set per Line per Event (see
-- https://github.com/cyrilleulmi/spongecoach/issues/1 follow-up rework). Drops the Focus catalog
-- entirely (its Line and Development-goal associations included) and replaces
-- line_focus_event.focus_id with a plain text column, carrying over existing seed text first.

alter table line_focus_event add column focus text;

update line_focus_event lfe
set focus = f.name
from focus f
where f.id = lfe.focus_id;

alter table line_focus_event drop column focus_id;
alter table line_focus_event alter column focus set not null;

drop table line_focus;
drop table focus_development_goal;
drop table focus;
