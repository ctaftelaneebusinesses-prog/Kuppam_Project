"""
Keyword-based category classifier for Business listings.

Used to (re)assign a specific Business.category value from a listing's name,
for cases where bulk data was tagged with a broad/legacy bucket (e.g. every
row in an old "Education" upload forced to one category regardless of
whether it's actually a school or a college).
"""
import re

COLLEGE_KEYWORDS = [
    'college', 'university', 'institute', 'polytechnic', 'degree',
]
SCHOOL_KEYWORDS = [
    'school', 'kidzee', 'pre school', 'preschool', 'vidyashram', 'kindergarten',
]
HOSPITAL_KEYWORDS = [
    'hospital', 'clinic', 'nursing home', 'multispeciality', 'multi speciality',
    'healthcare', 'health care', 'diagnostic', 'maternity', 'ivf', 'fertility',
]
PHARMACY_KEYWORDS = [
    'pharmacy', 'medicals', 'medical store', 'druggist', 'chemist', 'marunthagam',
]


def _has_any(text, keywords):
    return any(re.search(re.escape(k), text) for k in keywords)


def classify_education_bucket(name):
    """A record bulk-tagged as the legacy generic 'education' category: split into school vs college."""
    n = name.lower()
    if _has_any(n, COLLEGE_KEYWORDS):
        return 'college'
    return 'school'


def classify_hospital_bucket(name):
    """
    A record tagged 'hospital': confirms it's actually healthcare, and
    rescues rows that were clearly mis-filed (e.g. schools uploaded through
    the wrong template) or that are really pharmacies/medical stores.
    """
    n = name.lower()
    if _has_any(n, HOSPITAL_KEYWORDS):
        return 'hospital'
    if _has_any(n, SCHOOL_KEYWORDS):
        return 'school'
    if _has_any(n, COLLEGE_KEYWORDS):
        return 'college'
    if _has_any(n, PHARMACY_KEYWORDS):
        return 'pharmacy'
    return 'hospital'
