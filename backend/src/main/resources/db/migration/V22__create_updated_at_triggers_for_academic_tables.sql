CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER update_academic_years_updated_at BEFORE UPDATE ON academic_years FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_classes_updated_at BEFORE UPDATE ON classes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_streams_updated_at BEFORE UPDATE ON streams FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_subjects_updated_at BEFORE UPDATE ON subjects FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_teacher_profiles_updated_at BEFORE UPDATE ON teacher_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_teacher_assignments_updated_at BEFORE UPDATE ON teacher_assignments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_student_profiles_updated_at BEFORE UPDATE ON student_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_parents_updated_at BEFORE UPDATE ON parents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_student_parents_updated_at BEFORE UPDATE ON student_parents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_student_enrollments_updated_at BEFORE UPDATE ON student_enrollments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_attendance_updated_at BEFORE UPDATE ON attendance FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_examination_types_updated_at BEFORE UPDATE ON examination_types FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_examinations_updated_at BEFORE UPDATE ON examinations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_examination_subjects_updated_at BEFORE UPDATE ON examination_subjects FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_grading_schemes_updated_at BEFORE UPDATE ON grading_schemes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_grading_scheme_ranges_updated_at BEFORE UPDATE ON grading_scheme_ranges FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_results_updated_at BEFORE UPDATE ON results FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_result_approvals_updated_at BEFORE UPDATE ON result_approvals FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE OR REPLACE TRIGGER update_examination_reports_updated_at BEFORE UPDATE ON examination_reports FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
