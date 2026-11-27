-- seed_cq.sql
-- Demo creative questions (CQ) for Class 6 Mathematics, chapters 1-2.
-- Run AFTER seed_data.sql (it needs the chapters created there). Safe to re-run.
--
-- IMPORTANT (Spec 4.5/4.7): these are SAMPLE questions marked rubric.source = 'seed-demo'.
-- Replace with verified, reviewed NCTB-aligned content before any real release.
-- To remove them:  delete from creative_questions where rubric->>'source' = 'seed-demo';

insert into public.creative_questions (chapter_id, stem_en, stem_bn, subquestions, rubric, total_marks, status)
select c.id, q.stem_en, q.stem_bn, q.subquestions::jsonb, '{"source":"seed-demo"}'::jsonb, 10, 'published'
from public.chapters c
join public.subjects s on s.id = c.subject_id
join public.curriculum_versions v on v.id = s.curriculum_id
join (values
    (1,
     'Rina buys 3 pencils at x taka each and 2 erasers at 5 taka each.',
     'রিনা প্রতিটি x টাকা দরে ৩টি পেন্সিল এবং প্রতিটি ৫ টাকা দরে ২টি রাবার কেনে।',
     '[
       {"label":"ক","text_en":"What is a variable?","text_bn":"চলক কী?","marks":1,
        "model_answer_en":"A variable is a letter or symbol, such as x, whose value can change or is not known yet.",
        "model_answer_bn":"যে অক্ষর বা প্রতীকের (যেমন x) মান পরিবর্তন হতে পারে বা অজানা থাকে, তাকে চলক বলে।"},
       {"label":"খ","text_en":"Write the total cost as an algebraic expression.","text_bn":"মোট খরচকে বীজগণিতীয় রাশির মাধ্যমে প্রকাশ করো।","marks":2,
        "model_answer_en":"Pencils cost 3 x x = 3x taka and erasers cost 2 x 5 = 10 taka, so the total cost is 3x + 10 taka.",
        "model_answer_bn":"পেন্সিলের দাম ৩ x x = 3x টাকা এবং রাবারের দাম ২ x ৫ = ১০ টাকা। সুতরাং মোট খরচ 3x + ১০ টাকা।"},
       {"label":"গ","text_en":"If one pencil costs 8 taka, find the total cost.","text_bn":"একটি পেন্সিলের দাম ৮ টাকা হলে মোট খরচ নির্ণয় করো।","marks":3,
        "model_answer_en":"Put x = 8 in 3x + 10. Total cost = 3 x 8 + 10 = 24 + 10 = 34 taka.",
        "model_answer_bn":"3x + ১০ এ x = ৮ বসিয়ে পাই, মোট খরচ = ৩ x ৮ + ১০ = ২৪ + ১০ = ৩৪ টাকা।"},
       {"label":"ঘ","text_en":"If Rina pays 40 taka in total, find the price of one pencil and explain your steps.","text_bn":"রিনা মোট ৪০ টাকা দিলে একটি পেন্সিলের দাম নির্ণয় করো এবং ধাপগুলো ব্যাখ্যা করো।","marks":4,
        "model_answer_en":"Total cost = 40, so 3x + 10 = 40. Subtract 10 from both sides: 3x = 30. Divide both sides by 3: x = 10. One pencil costs 10 taka. Check: 3 x 10 + 10 = 40.",
        "model_answer_bn":"মোট খরচ ৪০ টাকা, তাই 3x + ১০ = ৪০। উভয় পক্ষ থেকে ১০ বিয়োগ করে পাই 3x = ৩০। উভয় পক্ষকে ৩ দিয়ে ভাগ করে পাই x = ১০। একটি পেন্সিলের দাম ১০ টাকা। যাচাই: ৩ x ১০ + ১০ = ৪০।"}
     ]',
     1),
    (2,
     'Karim thinks of a number. When he doubles it and adds 5, the result is 17.',
     'করিম একটি সংখ্যা ভাবল। সংখ্যাটিকে দ্বিগুণ করে তার সাথে ৫ যোগ করলে ১৭ হয়।',
     '[
       {"label":"ক","text_en":"What is an equation?","text_bn":"সমীকরণ কী?","marks":1,
        "model_answer_en":"An equation is a statement that two expressions are equal, joined by an equal sign. For example, x + 5 = 12.",
        "model_answer_bn":"সমান চিহ্ন দিয়ে দুটি রাশিকে সমান বোঝালে তাকে সমীকরণ বলে। যেমন, x + ৫ = ১২।"},
       {"label":"খ","text_en":"Write Karim''s statement as an equation.","text_bn":"করিমের কথাটিকে সমীকরণ আকারে লেখো।","marks":2,
        "model_answer_en":"Let the number be x. Doubling gives 2x and adding 5 gives 2x + 5. So the equation is 2x + 5 = 17.",
        "model_answer_bn":"মনে করি সংখ্যাটি x। দ্বিগুণ করলে 2x এবং ৫ যোগ করলে 2x + ৫ হয়। সুতরাং সমীকরণটি 2x + ৫ = ১৭।"},
       {"label":"গ","text_en":"Solve the equation to find Karim''s number.","text_bn":"সমীকরণটি সমাধান করে করিমের সংখ্যাটি নির্ণয় করো।","marks":3,
        "model_answer_en":"2x + 5 = 17. Subtract 5 from both sides: 2x = 12. Divide both sides by 2: x = 6. Karim''s number is 6.",
        "model_answer_bn":"2x + ৫ = ১৭। উভয় পক্ষ থেকে ৫ বিয়োগ করে পাই 2x = ১২। উভয় পক্ষকে ২ দিয়ে ভাগ করে পাই x = ৬। করিমের সংখ্যাটি ৬।"},
       {"label":"ঘ","text_en":"Check your answer. Then write and solve a similar equation for a number that gives 14 when tripled and reduced by 4.","text_bn":"উত্তরটি যাচাই করো। এরপর যে সংখ্যাকে তিনগুণ করে ৪ বিয়োগ করলে ১৪ হয়, তার জন্য অনুরূপ সমীকরণ লিখে সমাধান করো।","marks":4,
        "model_answer_en":"Check: 2 x 6 + 5 = 12 + 5 = 17, which is correct. For the new number y: 3y - 4 = 14. Add 4 to both sides: 3y = 18. Divide both sides by 3: y = 6. Check: 3 x 6 - 4 = 14.",
        "model_answer_bn":"যাচাই: ২ x ৬ + ৫ = ১২ + ৫ = ১৭, যা সঠিক। নতুন সংখ্যা y ধরলে 3y - ৪ = ১৪। উভয় পক্ষে ৪ যোগ করে পাই 3y = ১৮। উভয় পক্ষকে ৩ দিয়ে ভাগ করে পাই y = ৬। যাচাই: ৩ x ৬ - ৪ = ১৪।"}
     ]',
     2)
) as q(chapter_order, stem_en, stem_bn, subquestions, sort_key) on q.chapter_order = c.sort_order
where v.code = 'nctb-2023'
  and s.name_en = 'Mathematics' and s.class_level = 6 and s.medium = 'bangla'
  and not exists (
      select 1 from public.creative_questions cq
      where cq.chapter_id = c.id and cq.stem_en = q.stem_en
  );
