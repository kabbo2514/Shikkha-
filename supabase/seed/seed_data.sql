-- seed_data.sql
-- Demo content so the app is usable immediately after setup.
-- IMPORTANT (Spec 4.5/4.7): these are SAMPLE questions, source='seed-demo'.
-- Replace with verified, reviewed NCTB-aligned content before any real release.

-- ---------------------------------------------------------------------------
-- Curriculum version
-- ---------------------------------------------------------------------------
insert into public.curriculum_versions (code, name_en, name_bn, is_active)
values ('nctb-2023', 'NCTB Curriculum 2023', 'এনসিটিবি শিক্ষাক্রম ২০২৩', true)
on conflict (code) do nothing;

-- ---------------------------------------------------------------------------
-- Subjects. Availability is class + medium dependent (Spec 4.2).
-- ---------------------------------------------------------------------------
insert into public.subjects (curriculum_id, class_level, medium, name_en, name_bn, sort_order)
select v.id, x.class_level, x.medium, x.name_en, x.name_bn, x.sort_order
from public.curriculum_versions v
cross join (values
    -- Class 6, Bangla medium
    (6, 'bangla', 'Bangla', 'বাংলা', 1),
    (6, 'bangla', 'English', 'ইংরেজি', 2),
    (6, 'bangla', 'Mathematics', 'গণিত', 3),
    (6, 'bangla', 'Science', 'বিজ্ঞান', 4),
    (6, 'bangla', 'ICT', 'তথ্য ও যোগাযোগ প্রযুক্তি', 5),
    (6, 'bangla', 'Bangladesh and Global Studies', 'বাংলাদেশ ও বিশ্ব পরিচয়', 6),
    (6, 'bangla', 'Islamic Religious Studies', 'ইসলাম শিক্ষা', 7),
    -- Class 6, English medium
    (6, 'english', 'English', 'ইংরেজি', 1),
    (6, 'english', 'Mathematics', 'গণিত', 2),
    (6, 'english', 'Science', 'বিজ্ঞান', 3),
    (6, 'english', 'ICT', 'তথ্য ও যোগাযোগ প্রযুক্তি', 4),
    -- Class 9-10, Bangla medium, science group
    (9, 'bangla', 'Bangla First Paper', 'বাংলা প্রথম পত্র', 1),
    (9, 'bangla', 'Bangla Second Paper', 'বাংলা দ্বিতীয় পত্র', 2),
    (9, 'bangla', 'English First Paper', 'ইংরেজি প্রথম পত্র', 3),
    (9, 'bangla', 'English Second Paper', 'ইংরেজি দ্বিতীয় পত্র', 4),
    (9, 'bangla', 'Mathematics', 'গণিত', 5),
    (9, 'bangla', 'Higher Mathematics', 'উচ্চতর গণিত', 6),
    (9, 'bangla', 'Physics', 'পদার্থবিজ্ঞান', 7),
    (9, 'bangla', 'Chemistry', 'রসায়ন', 8),
    (9, 'bangla', 'Biology', 'জীববিজ্ঞান', 9),
    (9, 'bangla', 'ICT', 'তথ্য ও যোগাযোগ প্রযুক্তি', 10),
    (9, 'bangla', 'Bangladesh and Global Studies', 'বাংলাদেশ ও বিশ্বপরিচয়', 11),
    (9, 'bangla', 'Islamic Religious Studies', 'ইসলাম ও নৈতিক শিক্ষা', 12)
) as x(class_level, medium, name_en, name_bn, sort_order)
where v.code = 'nctb-2023'
on conflict (curriculum_id, class_level, medium, name_en) do nothing;

-- Class 10 shares the class-9 catalog rows for this demo.
insert into public.subjects (curriculum_id, class_level, medium, name_en, name_bn, sort_order)
select v.id, 10, x.medium, x.name_en, x.name_bn, x.sort_order
from public.curriculum_versions v
cross join (values
    ('bangla', 'Bangla First Paper', 'বাংলা প্রথম পত্র', 1),
    ('bangla', 'Bangla Second Paper', 'বাংলা দ্বিতীয় পত্র', 2),
    ('bangla', 'English First Paper', 'ইংরেজি প্রথম পত্র', 3),
    ('bangla', 'English Second Paper', 'ইংরেজি দ্বিতীয় পত্র', 4),
    ('bangla', 'Mathematics', 'গণিত', 5),
    ('bangla', 'Higher Mathematics', 'উচ্চতর গণিত', 6),
    ('bangla', 'Physics', 'পদার্থবিজ্ঞান', 7),
    ('bangla', 'Chemistry', 'রসায়ন', 8),
    ('bangla', 'Biology', 'জীববিজ্ঞান', 9),
    ('bangla', 'ICT', 'তথ্য ও যোগাযোগ প্রযুক্তি', 10),
    ('bangla', 'Bangladesh and Global Studies', 'বাংলাদেশ ও বিশ্বপরিচয়', 11),
    ('bangla', 'Islamic Religious Studies', 'ইসলাম ও নৈতিক শিক্ষা', 12)
) as x(medium, name_en, name_bn, sort_order)
where v.code = 'nctb-2023'
on conflict (curriculum_id, class_level, medium, name_en) do nothing;

-- ---------------------------------------------------------------------------
-- Chapters: Class 6 Mathematics (demo set)
-- ---------------------------------------------------------------------------
insert into public.chapters (subject_id, name_en, name_bn, sort_order)
select s.id, x.name_en, x.name_bn, x.sort_order
from public.subjects s
join public.curriculum_versions v on v.id = s.curriculum_id
cross join (values
    ('Algebraic Expressions', 'বীজগণিতীয় রাশি', 1),
    ('Simple Equations', 'সরল সমীকরণ', 2),
    ('Geometric Shapes', 'জ্যামিতিক আকৃতি', 3)
) as x(name_en, name_bn, sort_order)
where v.code = 'nctb-2023'
  and s.name_en = 'Mathematics' and s.class_level = 6 and s.medium = 'bangla'
on conflict do nothing;

-- ---------------------------------------------------------------------------
-- Sample MCQs (Class 6 Math, chapters 1-2). SOURCE: seed-demo — replace with
-- reviewed content before release.
-- ---------------------------------------------------------------------------
insert into public.mcqs (chapter_id, question_en, question_bn, options, correct_index, explanation_en, explanation_bn, difficulty, source, status)
select c.id, q.question_en, q.question_bn, q.options::jsonb, q.correct_index, q.explanation_en, q.explanation_bn, q.difficulty, 'seed-demo', 'published'
from public.chapters c
join public.subjects s on s.id = c.subject_id
join public.curriculum_versions v on v.id = s.curriculum_id
cross join (values
    -- Chapter 1: Algebraic Expressions
    ('Which of the following is an algebraic expression?',
     'নিচের কোনটি বীজগণিতীয় রাশি?',
     '[{"text_en":"3 + 5","text_bn":"৩ + ৫"},{"text_en":"x + 3","text_bn":"x + ৩"},{"text_en":"7","text_bn":"৭"},{"text_en":"10 - 2","text_bn":"১০ - ২"}]',
     1,
     'An algebraic expression contains variables (letters). x + 3 has the variable x.',
     'বীজগণিতীয় রাশিতে চলক (অক্ষর) থাকে। x + ৩ এ x চলক আছে।',
     1),
    ('What is the value of 2a + 3 when a = 4?',
     'a = ৪ হলে 2a + ৩ এর মান কত?',
     '[{"text_en":"8","text_bn":"৮"},{"text_en":"11","text_bn":"১১"},{"text_en":"14","text_bn":"১৪"},{"text_en":"24","text_bn":"২৪"}]',
     1,
     '2a + 3 = 2 x 4 + 3 = 8 + 3 = 11.',
     '2a + ৩ = ২ x ৪ + ৩ = ৮ + ৩ = ১১।',
     2),
    ('How many terms are in the expression 3x + 2y - 5?',
     '3x + 2y - 5 রাশিটিতে কয়টি পদ আছে?',
     '[{"text_en":"2","text_bn":"২"},{"text_en":"3","text_bn":"তিনটি"},{"text_en":"4","text_bn":"চারটি"},{"text_en":"5","text_bn":"পাঁচটি"}]',
     1,
     'The terms are 3x, 2y and -5, so there are 3 terms.',
     'পদগুলো হলো 3x, 2y এবং -5, অর্থাৎ তিনটি পদ।',
     2),
    ('If x = 3 and y = 2, what is x + y?',
     'x = ৩ এবং y = ২ হলে x + y এর মান কত?',
     '[{"text_en":"5","text_bn":"৫"},{"text_en":"6","text_bn":"৬"},{"text_en":"32","text_bn":"৩২"},{"text_en":"1","text_bn":"১"}]',
     0,
     'x + y = 3 + 2 = 5.',
     'x + y = ৩ + ২ = ৫।',
     1),
    ('Which of these is a constant term?',
     'নিচের কোনটি ধ্রুবক পদ?',
     '[{"text_en":"5x","text_bn":"5x"},{"text_en":"y","text_bn":"y"},{"text_en":"9","text_bn":"৯"},{"text_en":"2ab","text_bn":"2ab"}]',
     2,
     'A constant has a fixed value with no variables. 9 is a constant.',
     'ধ্রুবকের মান স্থির থাকে, তাতে চলক থাকে না। ৯ একটি ধ্রুবক।',
     1),
    ('Simplify: 4x - x.',
     'সরল করুন: 4x - x.',
     '[{"text_en":"4","text_bn":"৪"},{"text_en":"3x","text_bn":"3x"},{"text_en":"x","text_bn":"x"},{"text_en":"5x","text_bn":"5x"}]',
     1,
     '4x - x = (4 - 1)x = 3x.',
     '4x - x = (৪ - ১)x = 3x।',
     1),
    -- Chapter 2: Simple Equations
    ('Solve: x + 5 = 12.',
     'সমাধান করুন: x + ৫ = ১২.',
     '[{"text_en":"x = 5","text_bn":"x = ৫"},{"text_en":"x = 7","text_bn":"x = ৭"},{"text_en":"x = 12","text_bn":"x = ১২"},{"text_en":"x = 17","text_bn":"x = ১৭"}]',
     1,
     'Subtract 5 from both sides: x = 12 - 5 = 7.',
     'উভয় পক্ষ থেকে ৫ বিয়োগ করলে: x = ১২ - ৫ = ৭।',
     1),
    ('Solve: 3x = 21.',
     'সমাধান করুন: 3x = ২১.',
     '[{"text_en":"x = 3","text_bn":"x = ৩"},{"text_en":"x = 7","text_bn":"x = ৭"},{"text_en":"x = 18","text_bn":"x = ১৮"},{"text_en":"x = 24","text_bn":"x = ২৪"}]',
     1,
     'Divide both sides by 3: x = 21 / 3 = 7.',
     'উভয় পক্ষকে ৩ দিয়ে ভাগ করলে: x = ২১ / ৩ = ৭।',
     1),
    ('Which value of x satisfies 2x + 1 = 9?',
     '2x + ১ = ৯ সমীকরণটি সিদ্ধ করে এমন x এর মান কোনটি?',
     '[{"text_en":"4","text_bn":"৪"},{"text_en":"5","text_bn":"৫"},{"text_en":"3","text_bn":"৩"},{"text_en":"8","text_bn":"৮"}]',
     0,
     '2x = 8, so x = 4.',
     '2x = ৮, সুতরাং x = ৪।',
     2),
    ('An equation has how many sides separated by the equal sign?',
     'সমীকরণে সমান চিহ্নের দুই পাশে কয়টি পক্ষ থাকে?',
     '[{"text_en":"One","text_bn":"একটি"},{"text_en":"Two","text_bn":"দুটি"},{"text_en":"Three","text_bn":"তিনটি"},{"text_en":"Four","text_bn":"চারটি"}]',
     1,
     'An equation has a left-hand side and a right-hand side.',
     'সমীকরণের বামপক্ষ ও ডানপক্ষ—এই দুটি পক্ষ থাকে।',
     1)
) as q(question_en, question_bn, options, correct_index, explanation_en, explanation_bn, difficulty)
where v.code = 'nctb-2023'
  and s.name_en = 'Mathematics' and s.class_level = 6 and s.medium = 'bangla'
  and (
    (c.sort_order = 1 and q.question_en in (
        'Which of the following is an algebraic expression?',
        'What is the value of 2a + 3 when a = 4?',
        'How many terms are in the expression 3x + 2y - 5?',
        'If x = 3 and y = 2, what is x + y?',
        'Which of these is a constant term?',
        'Simplify: 4x - x.'))
    or
    (c.sort_order = 2 and q.question_en in (
        'Solve: x + 5 = 12.',
        'Solve: 3x = 21.',
        'Which value of x satisfies 2x + 1 = 9?',
        'An equation has how many sides separated by the equal sign?'))
  )
on conflict do nothing;
