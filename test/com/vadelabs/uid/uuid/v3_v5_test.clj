(ns com.vadelabs.uid.uuid.v3-v5-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [com.vadelabs.uid.uuid.core :as uuid]))


(deftest check-v3-special-cases
  (testing "v3 special case correctness..."
    (is (= (uuid/v3 uuid/+null+ "")
           #uuid "4ae71336-e44b-39bf-b9d2-752e234818a5"))
    (is (= (uuid/v3 uuid/+namespace-x500+ "")
           #uuid "7AAF118C-F174-3EBA-9EC5-680CD791A020"))
    (is (= (uuid/v3 uuid/+namespace-oid+ "")
           #uuid "596B79DC-00DD-3991-A72F-D3696C38C64F"))
    (is (= (uuid/v3 uuid/+namespace-dns+ "")
           #uuid "C87EE674-4DDC-3EFE-A74E-DFE25DA5D7B3"))
    (is (= (uuid/v3 uuid/+namespace-url+ "")
           #uuid "14CDB9B4-DE01-3FAA-AFF5-65BC2F771745"))))


(deftest check-v5-special-cases
  (testing "v5 special case correctness..."
    (is (= (uuid/v5 uuid/+null+ "")
           #uuid "E129F27C-5103-5C5C-844B-CDF0A15E160D"))
    (is (= (uuid/v5 uuid/+namespace-x500+ "")
           #uuid "B4BDF874-8C03-5BD8-8FD7-5E409DFD82C0"))
    (is (= (uuid/v5 uuid/+namespace-oid+ "")
           #uuid "0A68EB57-C88A-5F34-9E9D-27F85E68AF4F"))
    (is (= (uuid/v5 uuid/+namespace-dns+ "")
           #uuid "4EBD0208-8328-5D69-8C44-EC50939C0967"))
    (is (= (uuid/v5 uuid/+namespace-url+ "")
           #uuid "1B4DB7EB-4057-5DDF-91E0-36DEC72071F5"))))


(deftest check-v3-deterministic
  (testing "v3 UUIDs are deterministic"
    (let [ns-uuid uuid/+namespace-dns+
          nm "example.com"
          u1 (uuid/v3 ns-uuid nm)
          u2 (uuid/v3 ns-uuid nm)]
      (is (= u1 u2))
      (is (= 3 (uuid/get-version u1)))
      (is (= 2 (uuid/get-variant u1))))))


(deftest check-v5-deterministic
  (testing "v5 UUIDs are deterministic"
    (let [ns-uuid uuid/+namespace-dns+
          nm "example.com"
          u1 (uuid/v5 ns-uuid nm)
          u2 (uuid/v5 ns-uuid nm)]
      (is (= u1 u2))
      (is (= 5 (uuid/get-version u1)))
      (is (= 2 (uuid/get-variant u1))))))


(deftest check-v3-v5-different
  (testing "v3 and v5 produce different UUIDs for same input"
    (let [ns-uuid uuid/+namespace-dns+
          nm "example.com"
          u3 (uuid/v3 ns-uuid nm)
          u5 (uuid/v5 ns-uuid nm)]
      (is (not= u3 u5)))))


(deftest check-namespace-uniqueness
  (testing "Different namespaces produce different UUIDs"
    (let [nm "test"
          u-dns (uuid/v5 uuid/+namespace-dns+ nm)
          u-url (uuid/v5 uuid/+namespace-url+ nm)
          u-oid (uuid/v5 uuid/+namespace-oid+ nm)]
      (is (not= u-dns u-url))
      (is (not= u-dns u-oid))
      (is (not= u-url u-oid)))))


(deftest check-v3-sample-cases
  (testing "v3 sample test cases from reference"
    (is (= (uuid/v3 uuid/+null+ " !\"#$%&'()*+,-./0123456789")
           #uuid "84527A03-63CA-381A-8AFB-CF4244EF61FE"))
    (is (= (uuid/v3 uuid/+null+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
           #uuid "1F1723EB-AEB7-32C6-9221-B43CF93434AE"))
    (is (= (uuid/v3 uuid/+namespace-dns+ "abcdefghijklmnopqrstuvwxyz")
           #uuid "E7684C6A-B70E-3531-9426-3BC6E033B0FE"))))


(deftest check-v5-sample-cases
  (testing "v5 sample test cases from reference"
    (is (= (uuid/v5 uuid/+null+ " !\"#$%&'()*+,-./0123456789")
           #uuid "242E6E8E-7545-5A07-A02F-326EC30CB6B6"))
    (is (= (uuid/v5 uuid/+null+ "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
           #uuid "009442DE-7929-5001-9022-0D340553A9D6"))
    (is (= (uuid/v5 uuid/+namespace-dns+ "abcdefghijklmnopqrstuvwxyz")
           #uuid "E1CDA567-F00A-578F-B4EC-F248BD2743B2"))))
