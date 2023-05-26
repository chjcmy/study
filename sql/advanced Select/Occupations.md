~~~sql
with base as (
select *, row_number() over (partition by occupation order by name) R from occupations
)

select
max(if(occupation="Doctor", name,NULL)) as Doctor,
max(if(occupation="Professor", name,NULL)) as Professor,
max(if(occupation="Singer",name,NULL)) as Singer,
max(if(occupation="Actor",name,NULL)) as Actor
from base
group by R;
~~~
