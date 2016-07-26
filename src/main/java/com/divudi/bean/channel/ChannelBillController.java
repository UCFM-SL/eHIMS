/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.DoctorSpecialityController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.common.WebUserController;
import com.divudi.data.ApplicationInstitution;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.ChannelBean;
import com.divudi.ejb.FinalVariables;
import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Area;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Patient;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Staff;
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.entity.memberShip.PaymentSchemeDiscount;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.AgentReferenceBookFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ChannelBillController implements Serializable {

    private BillSession billSession;
    private BillSession billSessionTmp;
    private String patientTabId = "tabNewPt";
    private Patient newPatient;
    private Area area;
    private Patient searchPatient;
    private String agentRefNo;
    private String settleAgentRefNo;
    private double amount;
    private boolean foriegn = false;
    boolean settleSucessFully = false;
    PaymentMethod paymentMethod;
    PaymentMethod settlePaymentMethod;
    PaymentMethod cancelPaymentMethod;
    PaymentMethod refundPaymentMethod;
    PaymentMethodData paymentMethodData;
    Institution institution;
    Institution institutionOnCallAgency;
    Institution settleInstitution;
    Bill printingBill;
    Staff toStaff;
    String errorText;
    PaymentScheme paymentScheme;
    double creditLimit;
    boolean activeCreditLimitPannel = false;
    String comment;
    String commentR;
    ///////////////////////////////////
    private List<BillFee> billFee;
    private List<BillFee> refundBillFee;
    /////////////////////////////////
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillSessionFacade billSessionFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    AgentReferenceBookFacade agentReferenceBookFacade;
    //////////////////////////////////
    @EJB
    private ServiceSessionBean serviceSessionBean;
    @EJB
    FinalVariables finalVariables;
    //////////////////////////////
    @Inject
    private SessionController sessionController;
    @Inject
    private BookingController bookingController;
    @Inject
    PriceMatrixController priceMatrixController;
    @Inject
    DoctorSpecialityController doctorSpecialityController;
    @Inject
    ChannelSearchController channelSearchController;
    @Inject
    WebUserController webUserController;
    //////////////////////////////
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private ChannelBean channelBean;
    List<BillItem> billItems;
    int patientSearchTab;

    public Patient getNewPatient() {
        if (newPatient == null) {
            newPatient = new Patient();
            newPatient.setPerson(new Person());
        }
        return newPatient;
    }

    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
    }

    public Patient getSearchPatient() {
        return searchPatient;
    }

    public void setSearchPatient(Patient searchPatient) {
        this.searchPatient = searchPatient;
    }

    private BillSession savePaidBillSession(Bill bill, BillItem billItem) {
        BillSession bs = new BillSession();
        bs.copy(getBillSession());
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());

        getBillSessionFacade().create(bs);
        return bs;

    }

    public void settleCredit() {
        errorText = "";
        if (errorCheckForSettle()) {
            settleSucessFully = false;
            return;
        }

        Bill b = savePaidBill();

        BillItem bi = savePaidBillItem(b);
        savePaidBillFee(b, bi);

        BillSession bs = savePaidBillSession(b, bi);
        getBillSession().setPaidBillSession(bs);
        getBillSessionFacade().edit(bs);
        getBillSessionFacade().edit(getBillSession());

        getBillSession().getBill().setPaidAmount(b.getPaidAmount());
        getBillSession().getBill().setBalance(0.0);
        getBillSession().getBill().setPaidBill(b);
        getBillFacade().edit(getBillSession().getBill());

        b.setSingleBillItem(bi);
        b.setSingleBillSession(bs);
        getBillFacade().edit(b);
        settleSucessFully = true;
        printingBill = getBillFacade().find(b.getId());
        for (BillFee bf : billSession.getBill().getBillFees()) {
            if (bf.getFee().getFeeType() == FeeType.Staff && (getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna || getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative)) {
                bf.setTmpChangedValue(bf.getFeeValue());
            }
            if (bf.getFee().getFeeType() == FeeType.Staff) {
                printingBill.setVatPlusStaffFee(printingBill.getVatPlusStaffFee() + bf.getFeeValue() + bf.getFeeVat());
            } else {
                printingBill.setVatPlusHosFee(printingBill.getVatPlusHosFee() + bf.getFeeValue() + bf.getFeeVat());
            }
        }
//        System.err.println("*** Channel Credit Bill Settled ***");
//        System.out.println("bs = " + bs);
//        System.out.println("getBillSession() = " + getBillSession().getName());
//        System.out.println("getBillSession().getBill() = " + getBillSession().getBill());
//        System.out.println("getBillSession().getBill().getPaidBill() = " + getBillSession().getBill().getPaidBill());
//        System.out.println("getBillSession().getPaidBillSession() = " + getBillSession().getPaidBillSession().getName());
//        System.out.println("getBillSession().getPaidBillSession().getBill() = " + getBillSession().getPaidBillSession().getBill());
//        System.err.println("*** Channel Credit Bill Settled ***");
//        editBillSession(b, bi);
        UtilityController.addSuccessMessage("Channel Booking Added");

    }

//
//    private void deductBallance() {
//        double tmp = getBilledTotalFee() - getAgentPay().getBilledFee().getFeeValue();
//        getBillSession().getBill().getFromInstitution().setBallance(getBillSession().getBill().getFromInstitution().getBallance() - tmp);
//        getInstitutionFacade().edit(getBillSession().getBill().getFromInstitution());
//    }
    private Bill savePaidBill() {
        Bill temp = new BilledBill();
        temp.copy(getBillSession().getBill());
        temp.copyValue(getBillSession().getBill());
        temp.setPaidAmount(getBillSession().getBill().getNetTotal());
        temp.setBalance(0.0);
        temp.setPaymentMethod(settlePaymentMethod);
        temp.setReferenceBill(getBillSession().getBill());
        temp.setBillType(BillType.ChannelPaid);
        String insId = generateBillNumberInsId(temp);
        temp.setInsId(insId);
        String deptId = generateBillNumberDeptId(temp);
        temp.setDeptId(deptId);
//        temp.setInsId(getBillSession().getBill().getInsId());
        temp.setBookingId(billNumberBean.bookingIdGenerator(sessionController.getInstitution(), temp));

        temp.setDepartment(getSessionController().getDepartment());
        temp.setInstitution(getSessionController().getInstitution());
        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());

        getBillFacade().create(temp);

        return temp;
    }

    private BillItem savePaidBillItem(Bill b) {
        BillItem bi = new BillItem();
        bi.copy(billSession.getBillItem());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        bi.setBill(b);
        getBillItemFacade().create(bi);

        return bi;
    }

    private void savePaidBillFee(Bill b, BillItem bi) {

        for (BillFee f : billSession.getBill().getBillFees()) {

            BillFee bf = new BillFee();
            bf.copy(f);
            bf.setCreatedAt(Calendar.getInstance().getTime());
            bf.setCreater(getSessionController().getLoggedUser());
            bf.setBill(b);
            bf.setBillItem(bi);
            getBillFeeFacade().create(bf);
        }
    }

    private void editBillSession(Bill b, BillItem bi) {
//        getBillSession().setBill(b);
//        getBillSession().setBillItem(bi);
//
//        getBillSessionFacade().edit(getBillSession());
    }

    private boolean errorCheckForSettle() {

        if (getBillSession().getBill().getPaymentMethod() == PaymentMethod.Credit) {
            if (getBillSession().getBill().getFromInstitution() != null
                    && getBillSession().getBill().getFromInstitution().getBallance()
                    - getBillSession().getBill().getTotal() < -getBillSession().getBill().getFromInstitution().getAllowedCredit()) {
                UtilityController.addErrorMessage("Agency Ballance is Not Enough");
                errorText = "Agency Ballance is Not Enough";
                return true;
            }
        }

        if (settlePaymentMethod == PaymentMethod.Agent && settleInstitution == null) {
            UtilityController.addErrorMessage("Please select Agency");
            errorText = "Please select Agency";
            return true;
        }

        Bill b = getBillFacade().find(getBillSession().getBill().getId());
        System.out.println("b = " + b);
        System.out.println("getBillSession().getBill() = " + getBillSession().getBill());
        System.out.println("b.getPaidBill() = " + b.getPaidBill());
        System.out.println("getBillSession().getBill().getPaidBill() = " + getBillSession().getBill().getPaidBill());
        if (b.getPaidBill() != null) {
            UtilityController.addErrorMessage("Please Refresh The channeling Interface,Because this Channel Already Paid.");
            errorText = "Please Refresh The channeling Interface,Because this Channel Already Paid.";
            return true;
        }

        return false;
    }

    private boolean errorCheckRefunding() {
//        if (getBillSession().getBill().getBillType().getParent() == BillType.ChannelCreditFlow) {
//            UtilityController.addSuccessMessage("Credit Bill Cant be Refunded");
//            return true;
//        }

//        if (getDoctorFee().getBilledFee().getFeeValue() < getDoctorFee().getRepayment().getFeeValue()
//                || getHospitalFee().getBilledFee().getFeeValue() < getHospitalFee().getRepayment().getFeeValue()
//                || getTax().getBilledFee().getFeeValue() < getTax().getRepayment().getFeeValue()
//                || getAgentPay().getBilledFee().getFeeValue() < getAgentPay().getRepayment().getFeeValue()) {
//            UtilityController.addSuccessMessage("You can't refund mor than paid fee");
//            return true;
//        }
        return false;
    }

    public void refundCashFlowBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return;
        }
        if (getCommentR() == null || getCommentR().trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a comment");
            return;
        }
        calRefundTotal();
        System.out.println("getRefundableTotal() = " + getRefundableTotal());
        if (getRefundableTotal() == 0.0) {
            UtilityController.addErrorMessage("Please enter Correct Refundable Amount");
            return;
        }

        if (checkPaid()) {
            UtilityController.addErrorMessage("Doctor Payment has paid");
            return;
        }

        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());
        commentR = null;
        UtilityController.addSuccessMessage("Sucesssfully Refunded.");
    }

    public void refundAgentBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return;
        }
        if (refundPaymentMethod == null) {
            UtilityController.addErrorMessage("Select Refund Payment Method");
            return;
        }
        if (getCommentR() == null || getCommentR().trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a comment");
            return;
        }
        if (checkPaid()) {
            UtilityController.addErrorMessage("Doctor Payment has paid");
            return;
        }

        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());
        UtilityController.addSuccessMessage("Sucesssfully Refunded.");
        refundPaymentMethod = null;
        commentR = null;
    }

    public void refundCreditPaidBill() {
        if (getBillSession() == null) {
            return;
        }

        if (getBillSession().getPaidBillSession() == null) {
            UtilityController.addErrorMessage("No Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return;
        }
        if (getCommentR() == null || getCommentR().trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a comment");
            return;
        }

        if (checkPaidForPaid()) {
            UtilityController.addErrorMessage("Doctor Payment has paid");
            return;
        }

        if (getBillSession().getBill().getBillFees() != null) {

            for (BillFee bf : getBillSession().getBill().getBillFees()) {

                if (bf.getTmpChangedValue() == null) {
                    continue;
                }

                switch (bf.getFee().getFeeType()) {
                    case OtherInstitution:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            UtilityController.addErrorMessage("Enter Lesser Amount for Agency Fee");
                            return;
                        }
                        break;
                    case Staff:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            UtilityController.addErrorMessage("Enter Lesser Amount for Doctor Fee");
                            return;
                        }
                        break;

                    case Service:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            UtilityController.addErrorMessage("Enter Lesser Amount for Scan Fee");
                            return;
                        }
                        break;

                    case OwnInstitution:
                        if (bf.getFeeValue() < bf.getTmpChangedValue()) {
                            UtilityController.addErrorMessage("Enter Lesser Amount for Hospital Fee");
                            return;
                        }
                        break;

                    default:
                        UtilityController.addErrorMessage("Enter Refund Amount");
                        break;

                }
            }

        }

//        refund(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession().getPaidBillSession());
        refund(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession().getBill().getBillFees(), getBillSession());
        commentR = null;
        UtilityController.addSuccessMessage("Sucesssfully Refunded.");
    }

    public void refund(Bill bill, BillItem billItem, List<BillFee> billFees, BillSession billSession) {
        calRefundTotal();

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        }

        if (bill.getPaidBill() == null) {
            System.out.println("bill = " + bill);
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            RefundBill rb = (RefundBill) createRefundBill(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                rb.setPaymentMethod(refundPaymentMethod);
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal * finalVariables.getVATPercentageWithAmount(), HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }

            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

        } else {
            RefundBill rb = (RefundBill) createRefundBill(bill);
            BillItem rBilItm = refundBillItems(billItem, rb);
            createReturnBillFee(billFees, rb, rBilItm);
            BillSession rSession = refundBillSession(billSession, rb, rBilItm);

            billSession.setReferenceBillSession(rSession);
            billSessionFacade.edit(billSession);

            bill.setRefunded(true);
            bill.setRefundedBill(rb);
            getBillFacade().edit(bill);

            RefundBill rpb = (RefundBill) createRefundBill(bill.getPaidBill());
            BillItem rpBilItm = refundBillItems(bill.getSingleBillItem(), rpb);
            createReturnBillFee(billFees, rpb, rpBilItm);
            BillSession rpSession = refundBillSession(billSession.getPaidBillSession(), rpb, rpBilItm);

            billSession.getPaidBillSession().setReferenceBillSession(rpSession);
            billSessionFacade.edit(billSession.getPaidBillSession());

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                rb.setPaymentMethod(refundPaymentMethod);
                if (refundPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(rb.getCreditCompany(), refundableTotal * finalVariables.getVATPercentageWithAmount(), HistoryType.ChannelBooking, rb, rBilItm, rSession, rSession.getBillItem().getAgentRefNo());
                }
            }

            bill.getPaidBill().setRefunded(true);
            bill.getPaidBill().setRefundedBill(rpb);
            getBillFacade().edit(bill.getPaidBill());

        }

    }
    List<BillFee> listBillFees;

    public void createBillfees(SelectEvent event) {
        BillSession bs = ((BillSession) event.getObject());
        System.err.println("LISTED");
        String sql;
        HashMap hm = new HashMap();
        sql = "Select bf From BillFee bf where bf.retired=false"
                + " and bf.billItem=:bt ";
        hm.put("bt", bs.getBillItem());

        listBillFees = billFeeFacade.findBySQL(sql, hm);
        billSession = bs;
        System.out.println("bs = " + bs);
        System.out.println("billSession = " + billSession);
        System.out.println("bookingController.getSelectedBillSession() = " + bookingController.getSelectedBillSession());
        billSession.getBill().setVatPlusStaffFee(0.0);
        billSession.getBill().setVatPlusHosFee(0.0);
        for (BillFee bf : billSession.getBill().getBillFees()) {
            if (bf.getFee().getFeeType() == FeeType.Staff && (getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna || getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative)) {
                bf.setTmpChangedValue(bf.getFeeValue());
            }
            if (bf.getFee().getFeeType() == FeeType.Staff) {
                billSession.getBill().setVatPlusStaffFee(billSession.getBill().getVatPlusStaffFee() + bf.getFeeValue() + bf.getFeeVat());
            } else {
                billSession.getBill().setVatPlusHosFee(billSession.getBill().getVatPlusHosFee() + bf.getFeeValue() + bf.getFeeVat());
            }
        }
        calRefundTotal();
    }

    public BookingController getBookingController() {
        return bookingController;
    }

    public void setBookingController(BookingController bookingController) {
        this.bookingController = bookingController;
    }

    public List<BillFee> getListBillFees() {
        return listBillFees;
    }

    public void setListBillFees(List<BillFee> listBillFees) {
        this.listBillFees = listBillFees;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    private boolean checkPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBillSession().getBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findBySQL(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean checkPaidForPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBillSession().getBill().getPaidBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findBySQL(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean errorCheckCancelling() {
        if (getBillSession() == null) {
            return true;
        }

        if (getBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return true;
        }

        if (getBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return true;
        }

        if (checkPaid()) {
            UtilityController.addErrorMessage("Doctor Payment has paid");
            return true;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    public void cancelCashFlowBill() {
        if (errorCheckCancelling()) {
            return;
        }
        if (webUserController.hasPrivilege("ChannelCashCancelRestriction") && getBillSession().getBill().getPaymentMethod() == PaymentMethod.Cash) {
            UtilityController.addErrorMessage("You Are Restricted For Cash Cancellation...");
            return;
        }
        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());
        comment = null;
    }

    public void cancelBookingBill() {
        if (errorCheckCancelling()) {
            return;
        }

        CancelledBill cb = createCancelBill(getBillSession().getBill());
        BillItem cItem = cancelBillItems(getBillSession().getBillItem(), cb);
        BillSession cbs = cancelBillSession(getBillSession(), cb, cItem);
        getBillSession().getBill().setCancelled(true);
        getBillSession().getBill().setCancelledBill(cb);
        getBillFacade().edit(getBillSession().getBill());
        getBillSession().setReferenceBillSession(cbs);
        billSessionFacade.edit(billSession);

        comment = null;
    }

    public void cancelAgentPaidBill() {
        System.out.println("getBillSession() = " + getBillSession());
        System.out.println("getBillSessionTmp() = " + getBillSessionTmp());
        if (getBillSessionTmp() != null) {
            setBillSession(getBillSessionTmp());
        } else {
            setBillSession(getBillSession());
        }
        System.out.println("getBillSession() = " + getBillSession());
        System.out.println("getBillSessionTmp() = " + getBillSessionTmp());
        System.out.println("getCancelPaymentMethod() = " + getCancelPaymentMethod());
        System.out.println("getComment() = " + getComment());
        System.out.println("bookingController.getCanPayMetTmp() = " + bookingController.getCanPayMetTmp());
        if (getBillSession() == null) {
            UtilityController.addErrorMessage("No BillSession");
            return;
        }

        if (getBillSession().getBill() == null) {
            UtilityController.addErrorMessage("No Bill To Cancel");
            return;
        }

        if (getBillSession().getPaidBillSession() == null) {
            if (getBillSession().getBillItem().getBill().getBalance() == 0.0) {
                getBillSession().setPaidBillSession(getBillSession());
            } else {
                UtilityController.addErrorMessage("No Paid. Can not cancel.");
                return;
            }
        }

        if (getBillSession().getPaidBillSession().getBill() == null) {
            UtilityController.addErrorMessage("No Paid Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return;
        }

        if (getCancelPaymentMethod() == null) {
            UtilityController.addErrorMessage("Select Payment Method");
            return;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a comment");
            return;
        }

        //cancel(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getPaidBillSession());
        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());
        cancelPaymentMethod = null;
        comment = null;
    }

    public void cancelCreditPaidBill() {
        if (getBillSession() == null) {
            UtilityController.addErrorMessage("No BillSession");
            return;
        }

        if (getBillSession().getBill() == null) {
            UtilityController.addErrorMessage("No Bill To Cancel");
            return;
        }

        if (getBillSession().getPaidBillSession() == null) {
            UtilityController.addErrorMessage("No Paid Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill() == null) {
            UtilityController.addErrorMessage("No Paid Paid Bill Session");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isRefunded()) {
            UtilityController.addErrorMessage("Already Refunded");
            return;
        }

        if (getBillSession().getPaidBillSession().getBill().isCancelled()) {
            UtilityController.addErrorMessage("Already Cancelled");
            return;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a comment");
            return;
        }
        if (webUserController.hasPrivilege("ChannelCashCancelRestriction") && getBillSession().getBill().getPaymentMethod() == PaymentMethod.Cash) {
            UtilityController.addErrorMessage("You Are Restricted For Cash Cancellation...");
            return;
        }
        if (checkPaidForPaid()) {
            UtilityController.addErrorMessage("Doctor Payment has paid");
            return;
        }

        cancel(getBillSession().getPaidBillSession().getBill(), getBillSession().getPaidBillSession().getBillItem(), getBillSession().getPaidBillSession());
        cancel(getBillSession().getBill(), getBillSession().getBillItem(), getBillSession());
        comment = null;

    }

    public void cancel(Bill bill, BillItem billItem, BillSession billSession) {
        if (errorCheckCancelling()) {
            return;
        }

        if ((bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) && bill.getPaidBill() == null) {
            bill.setPaidBill(bill);
            billFacade.edit(bill);
        }

        //dr. buddhika said
        if (bill.getPaidBill() == null) {
            System.out.println("bill.getPaidBill() = " + bill);
            return;
        }

        if (bill.getPaidBill().equals(bill)) {
            System.err.println("Cash Bill");
            CancelledBill cb = createCancelBill(bill);
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(billSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);

            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getVatPlusNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

            //Update BillSession        
            billSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(billSession);

        } else {
            System.err.println("Paid Bill");
            CancelledBill cb = createCancelBill(bill);
            System.out.println("billItem = " + billItem);
            BillItem cItem = cancelBillItems(billItem, cb);
            BillSession cbs = cancelBillSession(billSession, cb, cItem);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit(bill);
            billSession.setReferenceBillSession(cbs);
            billSessionFacade.edit(billSession);

            CancelledBill cpb = createCancelBill(bill.getPaidBill());
            BillItem cpItem = cancelBillItems(bill.getPaidBill().getSingleBillItem(), cpb);
            BillSession cpbs = cancelBillSession(billSession.getPaidBillSession(), cpb, cpItem);
            bill.getPaidBill().setCancelled(true);
            bill.getPaidBill().setCancelledBill(cpb);
            getBillFacade().edit(bill.getPaidBill());
            billSession.getPaidBillSession().setReferenceBillSession(cpbs);
            billSessionFacade.edit(billSession.getPaidBillSession());
            if (bill.getPaymentMethod() == PaymentMethod.Agent) {
                if (cancelPaymentMethod == PaymentMethod.Agent) {
                    updateBallance(cb.getCreditCompany(), Math.abs(bill.getVatPlusNetTotal()), HistoryType.ChannelBooking, cb, cItem, cbs, cbs.getBillItem().getAgentRefNo());
                }
            }

        }

        UtilityController.addSuccessMessage("Cancelled");

    }

//    private void cancelBillItemsOld(CancelledBill can) {
//        BillItem bi = getBillSession().getBillItem();
//
//        BillItem b = new BillItem();
//        b.setBill(can);
//
//        b.setNetValue(0 - bi.getNetValue());
//        b.setCreatedAt(new Date());
//        b.setCreater(getSessionController().getLoggedUser());
//
//        getBillItemFacade().create(b);
//
//        cancelBillFeeOld(can, b);
//    }
    private BillItem cancelBillItems(BillItem bi, CancelledBill can) {

        BillItem b = new BillItem();
        b.setBill(can);
        b.copy(bi);
        b.invertValue(bi);
        b.setCreatedAt(new Date());
        b.setCreater(getSessionController().getLoggedUser());

        getBillItemFacade().create(b);
        String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tmp = getBillFeeFacade().findBySQL(sql);
        System.out.println("tmp.size() = " + tmp.size());
        cancelBillFee(can, b, tmp);

        return b;
    }

    private BillSession cancelBillSession(BillSession billSession, CancelledBill can, BillItem canBillItem) {
        BillSession bs = new BillSession();
        bs.copy(billSession);
        bs.setBill(can);
        bs.setBillItem(canBillItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());
        getBillSessionFacade().create(bs);

        can.setSingleBillSession(bs);
        getBillFacade().edit(can);

        return bs;
    }

    private BillSession refundBillSession(BillSession billSession, Bill bill, BillItem billItem) {
        BillSession bs = new BillSession();
        bs.copy(billSession);
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());
        getBillSessionFacade().create(bs);

        bill.setSingleBillSession(bs);
        getBillFacade().edit(bill);

        return bs;
    }

    private CancelledBill createCancelBill(Bill bill) {
        CancelledBill cb = new CancelledBill();

        cb.copy(bill);
        cb.invertValue(bill);
        cb.setBilledBill(bill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

//        cb.setInsId(billNumberBean.institutionChannelBillNumberGenerator(sessionController.getInstitution(), cb));
        String insId = generateBillNumberInsId(cb);

        if (insId.equals("")) {
            return null;
        }
        cb.setInsId(insId);

        String deptId = generateBillNumberDeptId(cb);

        if (deptId.equals("")) {
            return null;
        }
        cb.setDeptId(deptId);
        getBillFacade().create(cb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            cb.setPaymentMethod(cancelPaymentMethod);
//            if (cancelPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(cb.getCreditCompany(), Math.abs(bill.getNetTotal()), HistoryType.ChannelBooking, cb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            cb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(cb);
        return cb;
    }

    private void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);
            bf.setBill(can);
            bf.setBillItem(bt);

            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
    }

//    private void addBallance(Bill cb, double amt) {
//        cb.getFromInstitution().setBallance(cb.getFromInstitution().getBallance() + amt);
//        getInstitutionFacade().edit(cb.getFromInstitution());
//    }
//
//    private void cancelBillFeeOld(Bill b, BillItem bt) {
//        if (getDoctorFee().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getDoctorFee(), getDoctorFee().getBilledFee().getFeeValue());
//        }
//
//        if (getHospitalFee().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getHospitalFee(), getHospitalFee().getBilledFee().getFeeValue());
//        }
//
//        if (getTax().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getTax(), getTax().getBilledFee().getFeeValue());
//        }
//
//        if (getAgentPay().getRepayment().getFeeValue() != 0.0) {
//            createBillFee(b, bt, getAgentPay(), getAgentPay().getBilledFee().getFeeValue());
//        }
//
//    }
    public BillItem refundBillItems(BillItem bi, RefundBill rb) {
        BillItem rbi = new BillItem();
        rbi.copy(bi);
        rbi.resetValue();
        rbi.setBill(rb);
        rbi.setCreatedAt(Calendar.getInstance().getTime());
        rbi.setCreater(getSessionController().getLoggedUser());
        rbi.setItem(bi.getItem());
        rbi.setQty(0 - 1.0);
        rbi.setGrossValue(0 - getRefundableTotal());
        rbi.setNetValue(0 - getRefundableTotal());
        rbi.setVat(rbi.getNetValue() * finalVariables.getVATPercentage());
        rbi.setVatPlusNetValue(rbi.getNetValue() * finalVariables.getVATPercentageWithAmount());
        rbi.setReferanceBillItem(bi);
        getBillItemFacade().create(rbi);

        bi.setRefunded(Boolean.TRUE);
        getBillItemFacade().edit(bi);

        return rbi;

    }

    private void createReturnBillFee(List<BillFee> billFees, Bill b, BillItem bt) {
        double hf = 0.0;
        double sf = 0.0;
        for (BillFee bf : billFees) {
            if (bf.getTmpChangedValue() != null && bf.getTmpChangedValue() != 0) {
                BillFee newBf = new BillFee();
                newBf.copy(bf);
                newBf.setFeeGrossValue(0 - bf.getTmpChangedValue());
                newBf.setFeeValue(0 - bf.getTmpChangedValue());
                newBf.setFeeVat(newBf.getFeeValue() * finalVariables.getVATPercentage());
                newBf.setFeeVatPlusValue(newBf.getFeeValue() * finalVariables.getVATPercentageWithAmount());
                newBf.setBill(b);
                newBf.setBillItem(bt);
                newBf.setCreatedAt(new Date());
                newBf.setCreater(sessionController.getLoggedUser());
                billFeeFacade.create(newBf);

                if (bf.getFee().getFeeType() == FeeType.Staff) {
                    bt.setStaffFee(0 - bf.getTmpChangedValue());
                    sf += bt.getStaffFee();
                }

                if (bf.getFee().getFeeType() == FeeType.OwnInstitution) {
                    bt.setHospitalFee(0 - bf.getTmpChangedValue());
                    hf += bt.getHospitalFee();
                }

            }
        }
        b.setHospitalFee(hf);
        b.setStaffFee(sf);
        billFacade.edit(b);

        billItemFacade.edit(bt);
    }

    double refundableTotal;

    public double getRefundableTotal() {
        return refundableTotal;
    }

    public void setRefundableTotal(double refundableTotal) {
        this.refundableTotal = refundableTotal;
    }

    public void calRefundTotal() {
        refundableTotal = 0;
        for (BillFee bf : billSession.getBill().getBillFees()) {
            if (bf.getTmpChangedValue() != null) {
                refundableTotal += bf.getTmpChangedValue();
            }
        }
    }

    public void checkRefundTotal() {
        refundableTotal = 0;
        for (BillFee bf : billSession.getBill().getBillFees()) {
            if (bf.getTmpChangedValue() != null) {
                if (bf.getTmpChangedValue() > bf.getFeeValue()) {
                    bf.setTmpChangedValue(bf.getFeeValue());
                }
            }
        }

        calRefundTotal();
    }

    private Bill createRefundBill(Bill bill) {
        RefundBill rb = new RefundBill();
        rb.copy(bill);
        rb.setBilledBill(bill);
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setBilledBill(bill);
        rb.setComments(commentR);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getInstitution());

        rb.setNetTotal(0 - getRefundableTotal());
        rb.setTotal(0 - getRefundableTotal());
        rb.setVat(rb.getNetTotal() * finalVariables.getVATPercentage());
        rb.setVatPlusNetTotal(rb.getNetTotal() * finalVariables.getVATPercentageWithAmount());
        rb.setPaidAmount(0 - getRefundableTotal());

        String insId = generateBillNumberInsId(rb);

        if (insId.equals("")) {
            return null;
        }
        rb.setInsId(insId);

        String deptId = generateBillNumberDeptId(rb);

        if (deptId.equals("")) {
            return null;
        }
        rb.setDeptId(deptId);

        getBillFacade().create(rb);

        if (bill.getPaymentMethod() == PaymentMethod.Agent) {
            rb.setPaymentMethod(refundPaymentMethod);
//            if (refundPaymentMethod == PaymentMethod.Agent) {
//                updateBallance(rb.getCreditCompany(), refundableTotal, HistoryType.ChannelBooking, rb, billSession.getBillItem(), billSession, billSession.getBill().getReferralNumber());
//            }
        } else {
            rb.setPaymentMethod(bill.getPaymentMethod());
        }

        getBillFacade().edit(rb);

//Need To Update Agent BAllance
        return rb;

    }

    public void onTabChange(TabChangeEvent event) {
        //System.out.println("event : " + event.getTab().getId());
        setPatientTabId(event.getTab().getId());
    }

    public ChannelBillController() {
    }

    public ServiceSession getSs() {
        if (getbookingController().getSelectedServiceSession() != null) {
            return getServiceSessionFacade().findFirstBySQL("Select s From ServiceSession s where s.retired=false and s.id=" + getbookingController().getSelectedServiceSession().getId());
        } else {
            return new ServiceSession();
        }
    }

    public void updateBallance(Institution ins, double transactionValue, HistoryType historyType, Bill bill, BillItem billItem, BillSession billSession, String refNo) {
        System.out.println("updating agency balance");
        System.out.println("ins.getName() = " + ins.getName());
        System.out.println("ins.getBallance() before " + ins.getBallance());
        System.out.println("transactionValue = " + transactionValue);
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
        agentHistory.setCreater(getSessionController().getLoggedUser());
        agentHistory.setBill(bill);
        agentHistory.setBillItem(billItem);
        agentHistory.setBillSession(billSession);
        agentHistory.setBeforeBallance(ins.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setReferenceNo(refNo);
        agentHistory.setHistoryType(historyType);
        agentHistoryFacade.create(agentHistory);

        ins.setBallance(ins.getBallance() + transactionValue);
        getInstitutionFacade().edit(ins);

    }

    public double getAmount() {
        amount = 0.0;
        if (!foriegn) {
            amount = getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFee();
            System.err.println("getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFfee(); = " + getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFee());
        } else {
            amount = getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFfee();
            System.err.println("getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFfee(); = " + getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFfee());
        }
        return amount;
    }

    public double getAmountWithVAT() {
        amount = 0.0;
        if (!foriegn) {
            amount = getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFee() * finalVariables.getVATPercentageWithAmount();

            System.err.println("getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFfee(); = " + getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFee());
        } else {
            amount = getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFfee() * finalVariables.getVATPercentageWithAmount();
            System.err.println("getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFfee(); = " + getbookingController().getSelectedServiceSession().getOriginatingSession().getTotalFfee());
        }
        return amount;
    }

//    private List<Fee> getServiceSessionFee() {
//        List<Fee> tmp = new ArrayList<Fee>();
//
//        String sql = "select f From Fee f where f.retired=false and f.serviceSession.id=" + getbookingController().getSelectedServiceSession().getId();
//        if (getbookingController().getSelectedServiceSession() != null) {
//            tmp = getFeeFacade().findBySQL(sql);
//        }
//
//        return tmp;
//    }
//    private void saveBilledFee(Bill b, BillItem bi) {
//
//        for (Fee f : getServiceSessionFee()) {
//            if (f.getFee() == 0.0 && f.getFfee() == 0.0) {
//                continue;
//            }
//
//            if (getCurrent().getFromInstitution() == null && f.getFeeType() == FeeType.OtherInstitution) {
//                continue;
//            }
//
//            BillFee bf = new BillFee();
//
//            bf.setCreatedAt(Calendar.getInstance().getTime());
//            bf.setCreater(getSessionController().getLoggedUser());
//
//            if (bf.getPatienEncounter() != null) {
//                bf.setPatienEncounter(b.getPatientEncounter());
//            }
//
//            bf.setPatient(b.getPatient());
//            bf.setBill(b);
//            bf.setBillItem(bi);
//            bf.setFee(f);
//
//            if (f.getFeeType() == FeeType.Staff) {
//                bf.setStaff(f.getStaff());
//            }
//
//            if (f.getFeeType() == FeeType.OtherInstitution) {
//                bf.setInstitution(getCurrent().getFromInstitution());
//            }
//
//            if (foriegn) {
//                bf.setFeeValue(f.getFfee());
//            } else {
//                bf.setFeeValue(f.getFee());
//            }
//
//            getBillFeeFacade().create(bf);
//        }
//    }
    public void clearAll() {
        makeNull();
        makeNullSearchData();
    }

    public void makeNull() {
        System.err.println("make null");
        amount = 0.0;
        foriegn = false;
        billFee = null;
        refundBillFee = null;
        newPatient = null;
        searchPatient = null;
        printingBill = null;
        agentRefNo = "";
        billSession = null;
        patientTabId = "tabNewPt";
        patientSearchTab = 0;
        billFee = null;
        refundBillFee = null;
        billItems = null;
        paymentMethod = null;
        institution = null;
        institutionOnCallAgency = null;
        refundableTotal = 0;
        toStaff = null;
        paymentScheme = null;
        area = null;
        doctorSpecialityController.setSelectText("");
        bookingController.setSelectTextSpeciality("");
        bookingController.setSelectTextConsultant("");
        bookingController.setSelectTextSession("");
        comment = "";
        commentR = "";

    }

    public void makeNullSearchData() {
        channelSearchController.setFromDate(null);
        channelSearchController.setToDate(null);
        channelSearchController.setTxtSearch("");
        channelSearchController.setTxtSearchRef("");
        channelSearchController.setSearchedBillSessions(null);
    }

    @Inject
    AgentReferenceBookController agentReferenceBookController;

    private boolean errorCheck() {
        if (getbookingController().getSelectedServiceSession() == null) {
            errorText = "Please Select Specility and Doctor.";
            UtilityController.addErrorMessage("Please Select Specility and Doctor.");
            return true;
        }

        removeAgencyNullBill(getbookingController().getSelectedServiceSession());

        if (getbookingController().getSelectedServiceSession().isDeactivated()) {
            errorText = "******** Doctor Leave day Can't Channel ********";
            UtilityController.addErrorMessage("Doctor Leave day Can't Channel.");
            return true;
        }

        if (getbookingController().getSelectedServiceSession().getOriginatingSession() == null) {
            errorText = "Please Select Session.";
            UtilityController.addErrorMessage("Please Select Session");
            return true;
        }
        System.out.println("patientTabId = " + patientTabId);
        if (patientTabId.equals("tabNewPt")) {
            if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().equals("")) {
                errorText = "Can not bill without Patient.";
                UtilityController.addErrorMessage("Can't Settle Without Patient.");
                return true;
            }
            if ((getNewPatient().getPerson().getPhone() == null || getNewPatient().getPerson().getPhone().trim().equals("")) && !getSessionController().getInstitutionPreference().isChannelSettleWithoutPatientPhoneNumber()) {
                errorText = "Can not bill without Patient Contact Number.";
                UtilityController.addErrorMessage("Can't Settle Without Patient Contact Number.");
                return true;
            }
        }
        if (patientTabId.equals("tabSearchPt")) {
            if (getSearchPatient() == null) {
                errorText = "Please select Patient";
                UtilityController.addErrorMessage("Please select Patient");
                return true;
            }
        }

        if (paymentMethod == null) {
            errorText = "Please select Paymentmethod";
            UtilityController.addErrorMessage("Please select Paymentmethod");
            return true;
        }

        if (paymentMethod == PaymentMethod.Agent) {
            if (institution == null) {
                errorText = "Please select Agency";
                UtilityController.addErrorMessage("Please select Agency");
                return true;
            }
            System.out.println("amount = " + getAmount());
            System.out.println("institution.getBallance() = " + institution.getBallance());
            System.out.println("institution.getAllowedCredit() = " + institution.getAllowedCredit());
            if (institution.getBallance() - getAmount() < 0 - institution.getAllowedCredit()) {
                errorText = "Agency Ballance is Not Enough";
                UtilityController.addErrorMessage("Agency Ballance is Not Enough");
                return true;
            }
            if (getAgentReferenceBookController().checkAgentReferenceNumber(getAgentRefNo()) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "Invaild Reference Number.";
                UtilityController.addErrorMessage("Invaild Reference Number.");
                return true;
            }
            if (!getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                if (getAgentReferenceBookController().checkAgentReferenceNumberAlredyExsist(getAgentRefNo(), institution, BillType.ChannelAgent, PaymentMethod.Agent) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                    errorText = "This Reference Number( " + getAgentRefNo() + " ) is alredy Given.";
                    UtilityController.addErrorMessage("This Reference Number is alredy Given.");
                    setAgentRefNo("");
                    return true;
                }
                if (getAgentReferenceBookController().checkAgentReferenceNumber(institution, getAgentRefNo()) && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                    errorText = "This Reference Number is Blocked Or This channel Book is Not Issued.";
                    UtilityController.addErrorMessage("This Reference Number is Blocked Or This channel Book is Not Issued.");
                    return true;
                }
            }
        }
        if (paymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                errorText = "Please Select Staff.";
                JsfUtil.addErrorMessage("Please Select Staff.");
                return true;
            }
        }
        //System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        if (institution != null) {
            if (getAgentRefNo().trim().isEmpty() && !getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber()) {
                errorText = "Please Enter Agent Ref No";
                UtilityController.addErrorMessage("Please Enter Agent Ref No.");
                return true;
            }
        }
        //System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        if (getbookingController().getSelectedServiceSession().getStartingNo() > 0) {
            if (getSs().getMaxNo() != 0.0 && (getbookingController().getSelectedServiceSession().getTransDisplayCountWithoutCancelRefund() + getbookingController().getSelectedServiceSession().getStartingNo() - 1) >= getSs().getMaxNo()) {
                errorText = "No Space to Booking  This Channel.";
                UtilityController.addErrorMessage("No Space to Booking  This Channel.");
                return true;
            }
        } else {
            if (getSs().getMaxNo() != 0.0 && getbookingController().getSelectedServiceSession().getTransDisplayCountWithoutCancelRefund() >= getSs().getMaxNo()) {
                errorText = "No Space to Booking  This Channel.";
                UtilityController.addErrorMessage("No Space to Booking  This Channel.");
                return true;
            }
        }

        if (getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative) {
            if (paymentMethod == PaymentMethod.OnCall) {
                if (institutionOnCallAgency != null) {
                    if (institutionOnCallAgency.getBallance() != 0.0
                            || institutionOnCallAgency.getAllowedCredit() != 0.0
                            || institutionOnCallAgency.getMaxCreditLimit() != 0.0
                            || institutionOnCallAgency.getStandardCreditLimit() != 0.0) {
                        errorText = "You can't Add Booking to this Agent.This is Credit Agent.";
                        UtilityController.addErrorMessage("You can't Add Booking to this Agent.This is Credit Agent.");
                        return true;

                    }
                }
            }
        }

        //System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        return false;
    }

    private boolean errorCheckAgentValidate() {
        if (getbookingController().getSelectedServiceSession() == null) {
            errorText = "Please Select Specility and Doctor.";
            UtilityController.addErrorMessage("Please Select Specility and Doctor.");
            return true;
        }

        if (getbookingController().getSelectedServiceSession().isDeactivated()) {
            errorText = "******** Doctor Leave day Can't Channel ********";
            UtilityController.addErrorMessage("Doctor Leave day Can't Channel.");
            return true;
        }

        if (getbookingController().getSelectedServiceSession().getOriginatingSession() == null) {
            errorText = "Please Select Session.";
            UtilityController.addErrorMessage("Please Select Session");
            return true;
        }
        System.out.println("patientTabId = " + patientTabId);

        if (paymentMethod == PaymentMethod.Agent) {
            if (institution == null) {
                errorText = "Please select Agency";
                UtilityController.addErrorMessage("Please select Agency");
                return true;
            }

            if (institution.getBallance() - amount < 0 - institution.getAllowedCredit()) {
                errorText = "Agency Ballance is Not Enough";
                UtilityController.addErrorMessage("Agency Ballance is Not Enough");
                return true;
            }

        }

        //System.out.println("getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber() = " + getSessionController().getInstitutionPreference().isChannelWithOutReferenceNumber());
        return false;
    }

    private boolean errorCheckAfterSaveBill(Bill b) {
        if (b.getBillType() == BillType.ChannelAgent && b.getPaymentMethod() == PaymentMethod.Agent && b.getCreditCompany() == null) {
            return true;
        }

        if (b.getPaymentMethod() == null) {
            return true;
        }
        return false;
    }

    private void savePatient() {
        switch (getPatientTabId()) {
            case "tabNewPt":
                getNewPatient().setCreater(getSessionController().getLoggedUser());
                getNewPatient().setCreatedAt(new Date());
                getNewPatient().getPerson().setCreater(getSessionController().getLoggedUser());
                getNewPatient().getPerson().setCreatedAt(new Date());
                getPersonFacade().create(getNewPatient().getPerson());
                getPatientFacade().create(getNewPatient());
                break;
            case "tabSearchPt":
                break;
        }
    }

//    private BillItem saveBilledItem(Bill b) {
//        BillItem bi = new BillItem();
//        bi.setCreatedAt(new Date());
//        bi.setCreater(getSessionController().getLoggedUser());
//        bi.setBill(b);
//        bi.setAgentRefNo(agentRefNo);
//        bi.setNetValue(amount);
//        bi.setSessionDate(getbookingController().getSelectedServiceSession().getSessionAt());
//        getBillItemFacade().create(bi);
//
//        return bi;
//    }
//    private void saveBillSession(Bill b, BillItem bi) {
//        BillSession bs = new BillSession();
//        bs.setBill(b);
//        bs.setBillItem(bi);
//        bs.setCreatedAt(Calendar.getInstance().getTime());
//        bs.setCreater(getSessionController().getLoggedUser());
//        bs.setServiceSession(getbookingController().getSelectedServiceSession());
//        bs.setSessionDate(getbookingController().getSelectedServiceSession().getSessionAt());
//        bs.setSessionTime(getChannelBean().calSessionTime(getbookingController().getSelectedServiceSession()));
//
//        int count = getServiceSessionBean().getSessionNumber(getbookingController().getSelectedServiceSession(), getbookingController().getSelectedServiceSession().getSessionAt());
//        bs.setSerialNo(count);
//
//        getBillSessionFacade().create(bs);
//
//    }
    public void add() {
        errorText = "";
        if (errorCheck()) {
            settleSucessFully = false;
            return;
        }

        savePatient();
        printingBill = saveBilledBill();
        printingBill = getBillFacade().find(printingBill.getId());
        bookingController.fillBillSessions();
        bookingController.generateSessionsOnlyIdNew();
        for (BillFee bf : printingBill.getBillFees()) {
            if (bf.getFee().getFeeType() == FeeType.Staff && (getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna || getSessionController().getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative)) {
                bf.setTmpChangedValue(bf.getFeeValue());
            }
            if (bf.getFee().getFeeType() == FeeType.Staff) {
                printingBill.setVatPlusStaffFee(printingBill.getVatPlusStaffFee() + bf.getFeeValue() + bf.getFeeVat());
            } else {
                printingBill.setVatPlusHosFee(printingBill.getVatPlusHosFee() + bf.getFeeValue() + bf.getFeeVat());
            }
        }
        //********************retier bill,billitem,billsession***********************************************
        if (errorCheckAfterSaveBill(printingBill)) {

            printingBill.setRetired(true);
            printingBill.setRetireComments("Skip System Error");
            printingBill.setRetiredAt(new Date());
            getBillFacade().edit(printingBill);

            BillItem bi;
            BillSession bs;
            List<BillFee> BillFees;
            String sql;
            Map m = new HashMap();
            m.put("b", printingBill);
            if (printingBill.getSingleBillItem() != null) {
                bi = getBillItemFacade().find(printingBill.getSingleBillItem().getId());
            } else {
                sql = " select bi from billItem bi where "
                        + " bi.bill=:b ";
                bi = getBillItemFacade().findFirstBySQL(sql, m);
            }
            if (bi != null) {
                bi.setRetired(true);
                bi.setRetireComments("Skip System Error");
                bi.setRetirer(getSessionController().getLoggedUser());
                bi.setRetiredAt(new Date());
                getBillItemFacade().edit(bi);
            }

            if (printingBill.getSingleBillSession() != null) {
                bs = getBillSessionFacade().find(printingBill.getSingleBillSession().getId());
            } else {
                sql = " select bs from BillSession bs where "
                        + " bs.bill=:b ";
                bs = getBillSessionFacade().findFirstBySQL(sql, m);
            }
            if (bs != null) {
                bs.setRetired(true);
                bs.setRetireComments("Skip System Error");
                bs.setRetirer(getSessionController().getLoggedUser());
                bs.setRetiredAt(new Date());
                getBillSessionFacade().edit(bs);
            }

            sql = " select bf from BillFee bf where "
                    + " bf.bill=:b ";

            BillFees = getBillFeeFacade().findBySQL(sql, m);
            if (!BillFees.isEmpty()) {
                for (BillFee bf : BillFees) {
                    bf.setRetired(true);
                    bf.setRetireComments("Skip System Error");
                    bf.setRetirer(getSessionController().getLoggedUser());
                    bf.setRetiredAt(new Date());
                    getBillFeeFacade().edit(bf);
                }
            }
            System.err.println("Skiped System Error");
            return;
        }
        //********************retier bill,billitem,billsession***********************************************
        settleSucessFully = true;
        sessionController.setBill(printingBill);

        UtilityController.addSuccessMessage("Channel Booking Added.");
    }

    public void removeAgencyNullBill(ServiceSession ss) {
        List<BillSession> billSessions = new ArrayList<>();
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.serviceSession=:ss "
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and bs.sessionDate= :ssDate "
                + " order by bs.serialNo desc ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("ssDate", ss.getSessionDate());
        hh.put("ss", ss);
        billSessions = getBillSessionFacade().findBySQL(sql, hh, TemporalType.DATE, 5);

        for (BillSession bs : billSessions) {
            System.out.println("bs.getBill().getCreditCompany() = " + bs.getBill().getCreditCompany());
            System.out.println("bs.getBill().getBillType() = " + bs.getBill().getBillType());
            System.out.println("bs.getBill().getPaymentMethod() = " + bs.getBill().getPaymentMethod());
            if (errorCheckAfterSaveBill(bs.getBill())) {
                System.err.println("Error Bill");
                bs.getBill().setRetired(true);
                bs.getBill().setRetireComments("Skip System Error");
                bs.getBill().setRetiredAt(new Date());
                getBillFacade().edit(bs.getBill());

                BillItem bi;
                List<BillFee> BillFees;
                Map m = new HashMap();
                m.put("b", bs.getBill());
                if (bs.getBill().getSingleBillItem() != null) {
                    bi = getBillItemFacade().find(bs.getBill().getSingleBillItem().getId());
                } else {
                    sql = " select bi from BillItem bi where "
                            + " bi.bill=:b ";
                    bi = getBillItemFacade().findFirstBySQL(sql, m);
                }
                if (bi != null) {
                    bi.setRetired(true);
                    bi.setRetireComments("Skip System Error");
                    bi.setRetirer(getSessionController().getLoggedUser());
                    bi.setRetiredAt(new Date());
                    getBillItemFacade().edit(bi);
                }

                bs.setRetired(true);
                bs.setRetireComments("Skip System Error");
                bs.setRetirer(getSessionController().getLoggedUser());
                bs.setRetiredAt(new Date());
                getBillSessionFacade().edit(bs);

                sql = " select bf from BillFee bf where "
                        + " bf.bill=:b ";

                BillFees = getBillFeeFacade().findBySQL(sql, m);
                if (!BillFees.isEmpty()) {
                    for (BillFee bf : BillFees) {
                        bf.setRetired(true);
                        bf.setRetireComments("Skip System Error");
                        bf.setRetirer(getSessionController().getLoggedUser());
                        bf.setRetiredAt(new Date());
                        getBillFeeFacade().edit(bf);
                    }
                }
                System.err.println("Skiped System Error");
            }

        }

    }

    public void clearBillValues() {
        patientSearchTab = 0;
        paymentMethod = sessionController.getInstitutionPreference().getChannellingPaymentMethod();
    }

    public void addOnCall() {
        if (errorCheck()) {
            return;
        }

        if (printingBill == null) {
            printingBill = new Bill();
        }

        printingBill.setPaymentMethod(PaymentMethod.OnCall);
        add();

    }

    private BillSession createBillSession(Bill bill, BillItem billItem) {
        BillSession bs = new BillSession();
        bs.setAbsent(false);
        bs.setBill(bill);
        bs.setBillItem(billItem);
        bs.setCreatedAt(new Date());
        bs.setCreater(getSessionController().getLoggedUser());
        bs.setDepartment(getbookingController().getSelectedServiceSession().getOriginatingSession().getDepartment());
        bs.setInstitution(getbookingController().getSelectedServiceSession().getOriginatingSession().getInstitution());
        bs.setItem(getbookingController().getSelectedServiceSession());
//        bs.setItem(getbookingController().getSelectedServiceSession().getOriginatingSession());

//        bs.setPresent(true);
        System.out.println("getbookingController().getSelectedServiceSession().getOriginatingSession() = " + getbookingController().getSelectedServiceSession().getOriginatingSession());

        bs.setServiceSession(getbookingController().getSelectedServiceSession());
//        bs.setServiceSession(getbookingController().getSelectedServiceSession().getOriginatingSession());
        bs.setSessionDate(getbookingController().getSelectedServiceSession().getSessionDate());
        bs.setSessionTime(getbookingController().getSelectedServiceSession().getSessionTime());
        bs.setStaff(getbookingController().getSelectedServiceSession().getStaff());

        int count = getServiceSessionBean().getSessionNumber(getbookingController().getSelectedServiceSession(), getbookingController().getSelectedServiceSession().getSessionDate(), bs);
        System.err.println("count" + count);
        System.out.println("getbookingController().getSelectedServiceSession().getStartingNo() = " + getbookingController().getSelectedServiceSession().getStartingNo());
        System.out.println("getbookingController().getSelectedServiceSession().getOriginatingSession().getStartingNo() = " + getbookingController().getSelectedServiceSession().getOriginatingSession().getStartingNo());

        bs.setSerialNo(count);

        getBillSessionFacade().create(bs);

        return bs;
    }

    private BillItem createBillItem(Bill bill) {
        BillItem bi = new BillItem();
        bi.setAdjustedValue(0.0);
        bi.setAgentRefNo(agentRefNo);
        bi.setBill(bill);
        bi.setBillTime(new Date());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        bi.setGrossValue(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal());
        bi.setItem(getbookingController().getSelectedServiceSession());
//        bi.setItem(getbookingController().getSelectedServiceSession().getOriginatingSession());
        bi.setNetRate(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal());
        bi.setNetValue(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal());
        bi.setVat(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal() * finalVariables.getVATPercentage());
        bi.setVatPlusNetValue(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal() * finalVariables.getVATPercentageWithAmount());
        bi.setQty(1.0);
        bi.setRate(getbookingController().getSelectedServiceSession().getOriginatingSession().getTotal());
        bi.setSessionDate(getbookingController().getSelectedServiceSession().getSessionAt());

        billItemFacade.create(bi);
        return bi;
    }

    private List<BillFee> createBillFee(Bill bill, BillItem billItem) {
        List<BillFee> billFeeList = new ArrayList<>();
        double tmpTotal = 0;
        double tmpDiscount = 0;
        for (ItemFee f : getbookingController().getSelectedServiceSession().getOriginatingSession().getItemFees()) {
            if (paymentMethod != PaymentMethod.Agent) {
                if (f.getFeeType() == FeeType.OtherInstitution) {
                    continue;
                }
            }
            if (paymentMethod != PaymentMethod.OnCall) {
                if (f.getFeeType() == FeeType.OwnInstitution && f.getName().equalsIgnoreCase("On-Call Fee")) {
                    continue;
                }
            }
            BillFee bf = new BillFee();
            bf.setBill(bill);
            bf.setBillItem(billItem);
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());
            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(f.getInstitution());
                bf.setDepartment(f.getDepartment());
            } else if (f.getFeeType() == FeeType.OtherInstitution) {
                bf.setInstitution(institution);
            } else if (f.getFeeType() == FeeType.Staff) {
                bf.setSpeciality(f.getSpeciality());
                System.out.println("bf.getSpeciality() = " + bf.getSpeciality());
                bf.setStaff(f.getStaff());
                System.out.println("bf.getStaff() = " + bf.getStaff());
            }

            bf.setFee(f);
            bf.setFeeAt(new Date());
            bf.setFeeDiscount(0.0);
            bf.setOrderNo(0);
            bf.setPatient(bill.getPatient());

            if (bf.getPatienEncounter() != null) {
                bf.setPatienEncounter(bill.getPatientEncounter());
            }

            bf.setPatient(bill.getPatient());

            if (f.getFeeType() == FeeType.Staff) {
                bf.setStaff(f.getStaff());
            }

            if (f.getFeeType() == FeeType.OwnInstitution) {
                bf.setInstitution(sessionController.getInstitution());
            }

            PaymentSchemeDiscount paymentSchemeDiscount = priceMatrixController.fetchPaymentSchemeDiscount(paymentScheme, paymentMethod);
            double d = 0;
            if (foriegn) {
                bf.setFeeValue(f.getFfee());
            } else {
                bf.setFeeValue(f.getFee());
            }

            if (f.getFeeType() == FeeType.OwnInstitution && paymentSchemeDiscount != null) {
                d = bf.getFeeValue() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                bf.setFeeDiscount(d);
                bf.setFeeGrossValue(bf.getFeeValue());
                bf.setFeeValue(bf.getFeeGrossValue() - bf.getFeeDiscount());
                tmpDiscount += d;
            }
            bf.setFeeVat(bf.getFeeValue() * finalVariables.getVATPercentage());
            bf.setFeeVatPlusValue(bf.getFeeValue() * finalVariables.getVATPercentageWithAmount());

            tmpTotal += bf.getFeeValue();

//            if (paymentMethod.equals(PaymentMethod.Credit)) {
//                bf.setPaidValue(0.0);
//            } else {
//                bf.setPaidValue(bf.getFeeValue());
//            }
            billFeeFacade.create(bf);
            billFeeList.add(bf);
        }
        bill.setDiscount(tmpDiscount);
        bill.setNetTotal(tmpTotal);
        bill.setVat(tmpTotal * finalVariables.getVATPercentage());
        bill.setVatPlusNetTotal(tmpTotal * finalVariables.getVATPercentageWithAmount());
        System.out.println("tmpDiscount = " + tmpDiscount);
        System.out.println("tmpTotal = " + tmpTotal);
        System.out.println("bill.getNetTotal() = " + bill.getNetTotal());
        System.out.println("bill.getTotal() = " + bill.getTotal());
        getBillFacade().edit(bill);

        billItem.setDiscount(tmpDiscount);
        billItem.setNetValue(tmpTotal);
        billItem.setVat(tmpTotal * finalVariables.getVATPercentage());
        billItem.setVatPlusNetValue(tmpTotal * finalVariables.getVATPercentageWithAmount());
        System.out.println("billItem.getNetValue() = " + billItem.getNetValue());
        getBillItemFacade().edit(billItem);

//        if (paymentMethod != PaymentMethod.Agent) {
//            changeAgentFeeToHospitalFee();
//        }
        return billFeeList;

    }

    public void changeAgentFeeToHospitalFee() {
        List<ItemFee> itemFees = getbookingController().getSelectedServiceSession().getOriginatingSession().getItemFees();
        double agentFee = 0.0;
        double agentFfee = 0.0;
        for (ItemFee ifl : itemFees) {
            if (ifl.getFeeType() == FeeType.OtherInstitution) {
                agentFee = ifl.getFee();
                System.out.println("agentFee = " + agentFee);
                agentFfee = ifl.getFfee();

                ifl.setFee(0.0);
                ifl.setFfee(0.0);
            }
        }
        for (ItemFee ifl : itemFees) {
            if (ifl.getFeeType() == FeeType.OwnInstitution) {

                System.out.println("1.agentFee = " + agentFee);
                System.out.println("1.agentFfee = " + agentFfee);
                agentFee += ifl.getFee();
                agentFfee += ifl.getFfee();
                System.out.println("2.agentFee = " + agentFee);

                ifl.setFee(agentFee);
                ifl.setFfee(agentFfee);
            }
        }
    }

    public Bill getPrintingBill() {
        return printingBill;
    }

    public void setPrintingBill(Bill printingBill) {
        this.printingBill = printingBill;
    }

    private Bill createBill() {
        Bill bill = new BilledBill();
        bill.setStaff(getbookingController().getSelectedServiceSession().getOriginatingSession().getStaff());
        bill.setToStaff(toStaff);
        bill.setAppointmentAt(getbookingController().getSelectedServiceSession().getSessionDate());
        bill.setTotal(getAmount());
        bill.setVat(getAmount() * finalVariables.getVATPercentage());
        bill.setNetTotal(getAmount());
        bill.setVatPlusNetTotal(getAmountWithVAT());
        bill.setPaymentMethod(paymentMethod);

        System.out.println("getPatientTabId() = " + getPatientTabId());

        if (getPatientTabId().equals("tabNewPt")) {
            bill.setPatient(newPatient);
        } else {
            bill.setPatient(searchPatient);
        }

        switch (paymentMethod) {
            case OnCall:
                bill.setBillType(BillType.ChannelOnCall);
                //agent on-call record
                if (institutionOnCallAgency != null) {
                    System.out.println("institution.getName() = " + institutionOnCallAgency.getName());
                }
                bill.setCreditCompany(institutionOnCallAgency);
                break;
            case Cash:
                bill.setBillType(BillType.ChannelCash);
                break;

            case Card:
                bill.setBillType(BillType.ChannelCash);
                break;

            case Cheque:
                bill.setBillType(BillType.ChannelCash);
                break;

            case Slip:
                bill.setBillType(BillType.ChannelCash);
                break;
            case Agent:
                bill.setBillType(BillType.ChannelAgent);
                bill.setCreditCompany(institution);
                break;
            case Staff:
                bill.setBillType(BillType.ChannelStaff);
                break;
            case Credit:
                bill.setBillType(BillType.ChannelCredit);
                break;
        }

//        bill.setInsId(getBillNumberBean().institutionChannelBillNumberGenerator(sessionController.getInstitution(), bill));
        String insId = generateBillNumberInsId(bill);

        if (insId.equals("")) {
            return null;
        }
        bill.setInsId(insId);

        String deptId = generateBillNumberDeptId(bill);

        if (deptId.equals("")) {
            return null;
        }
        bill.setDeptId(deptId);

        if (bill.getBillType().getParent() == BillType.ChannelCashFlow) {
            bill.setBookingId(getBillNumberBean().bookingIdGenerator(sessionController.getInstitution(), new BilledBill()));
            bill.setPaidAmount(getAmount());
            bill.setPaidAt(new Date());
        }

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());
        bill.setCreater(getSessionController().getLoggedUser());
        System.out.println("Creater Bill" + getSessionController().getLoggedUser().getWebUserPerson().getName());
        bill.setDepartment(getSessionController().getDepartment());
        bill.setInstitution(sessionController.getInstitution());
        if (getbookingController() != null) {

            if (getbookingController().getSelectedServiceSession() != null) {

                if (getbookingController().getSelectedServiceSession().getDepartment() != null) {
                }
            }
        }

        bill.setToDepartment(getbookingController().getSelectedServiceSession().getDepartment());
        bill.setToInstitution(getbookingController().getSelectedServiceSession().getInstitution());

        getBillFacade().create(bill);

        if (bill.getBillType() == BillType.ChannelCash || bill.getBillType() == BillType.ChannelAgent) {
            System.out.println("paidBill 1= " + bill.getPaidBill());
            bill.setPaidBill(bill);
            getBillFacade().edit(bill);
        }

        return bill;
    }

    @Inject
    BillBeanController billBeanController;

    private Bill saveBilledBill() {
        Bill savingBill = createBill();
        BillItem savingBillItem = createBillItem(savingBill);
        BillSession savingBillSession = createBillSession(savingBill, savingBillItem);

        List<BillFee> savingBillFees = createBillFee(savingBill, savingBillItem);
        List<BillItem> savingBillItems = new ArrayList<>();
        savingBillItems.add(savingBillItem);

        getAmount();

        getBillItemFacade().edit(savingBillItem);

        //Update Bill Session
        savingBillItem.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBillItem));
        savingBillItem.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBillItem));
        savingBillItem.setBillSession(savingBillSession);
        getBillSessionFacade().edit(savingBillSession);

        //Update Bill
        savingBill.setHospitalFee(billBeanController.calFeeValue(FeeType.OwnInstitution, savingBill));
        savingBill.setStaffFee(billBeanController.calFeeValue(FeeType.Staff, savingBill));
        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);
        savingBill.setBillItems(savingBillItems);
        savingBill.setBillFees(savingBillFees);

        if (savingBill.getBillType() == BillType.ChannelAgent) {
            updateBallance(savingBill.getCreditCompany(), 0 - savingBill.getVatPlusNetTotal(), HistoryType.ChannelBooking, savingBill, savingBillItem, savingBillSession, savingBillItem.getAgentRefNo());
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelCash) {
            savingBill.setBalance(0.0);
            savingBillSession.setPaidBillSession(savingBillSession);
        } else if (savingBill.getBillType() == BillType.ChannelOnCall) {
            savingBill.setBalance(savingBill.getNetTotal());
        } else if (savingBill.getBillType() == BillType.ChannelStaff) {
            savingBill.setBalance(savingBill.getNetTotal());
        }

        savingBill.setSingleBillItem(savingBillItem);
        savingBill.setSingleBillSession(savingBillSession);

        getBillFacade().edit(savingBill);
        getBillSessionFacade().edit(savingBillSession);
        return savingBill;
    }

    private String generateBillNumberInsId(Bill bill) {
        String suffix = getSessionController().getInstitution().getInstitutionCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String insId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                insId = getBillNumberBean().institutionBillNumberGenerator(sessionController.getInstitution(), billType, billClassType, suffix);
            } else {
                if (sessionController.getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                    suffix += "CH";
                } else {
                    suffix += "CHANN";
                }
                insId = getBillNumberBean().institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            if (sessionController.getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                suffix += "CHCAN";
            } else {
                suffix += "CHANNCAN";
            }
            billClassType = BillClassType.CancelledBill;
            insId = getBillNumberBean().institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            if (sessionController.getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                suffix += "CHREF";
            } else {
                suffix += "CHANNREF";
            }
            billClassType = BillClassType.RefundBill;
            insId = getBillNumberBean().institutionBillNumberGenerator(sessionController.getInstitution(), bts, billClassType, suffix);
        }

        System.out.println("billClassType = " + billClassType);
        System.out.println("insId = " + insId);

        return insId;
    }

    private String generateBillNumberDeptId(Bill bill) {
        String suffix = getSessionController().getDepartment().getDepartmentCode();
        BillClassType billClassType = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        BillType billType = null;
        String deptId = null;
        if (bill instanceof BilledBill) {

            billClassType = BillClassType.BilledBill;
            if (bill.getBillType() == BillType.ChannelOnCall || bill.getBillType() == BillType.ChannelStaff) {
                billType = bill.getBillType();
                if (billType == BillType.ChannelOnCall) {
                    suffix += "BKONCALL";
                } else {
                    suffix += "BKSTAFF";
                }
                deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), billType, billClassType, suffix);
            } else {
                if (sessionController.getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                    suffix += "CH";
                } else {
                    suffix += "CHANN";
                }
                deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
            }
        }

        if (bill instanceof CancelledBill) {
            if (sessionController.getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                suffix += "CHCAN";
            } else {
                suffix += "CHANNCAN";
            }
            billClassType = BillClassType.CancelledBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
        }

        if (bill instanceof RefundBill) {
            if (sessionController.getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Ruhuna) {
                suffix += "CHREF";
            } else {
                suffix += "CHANNREF";
            }
            billClassType = BillClassType.RefundBill;
            deptId = getBillNumberBean().departmentBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), bts, billClassType, suffix);
        }

        System.out.println("billClassType = " + billClassType);
        System.out.println("deptId = " + deptId);

        return deptId;
    }

    public List<BillFee> getBillFee() {

        billFee = new ArrayList<>();
        if (billSession != null) {
            String sql = "Select s From BillFee s where s.retired=false and s.bill.id=" + billSession.getBill().getId();
            billFee = getBillFeeFacade().findBySQL(sql);
        }

        return billFee;
    }

    public List<BillFee> getRefundBillFee() {
        if (refundBillFee == null) {
            refundBillFee = new ArrayList<BillFee>();
            if (billSession != null) {
                //String sql = "Select s From BillFee s where s.retired=false and s.bill.id=" + billSession.getBill().getId();
                String sql = "Select s From BillFee s where s.retired=false and s.bill.billedBill.id=" + billSession.getBill().getId();
                refundBillFee = getBillFeeFacade().findBySQL(sql);
            }
        }
        return refundBillFee;
    }

    public void listnerSetBillSession(BillSession bs) {
        setBillSessionTmp(bs);
        setBillSession(bs);
    }

    public void setBillFee(List<BillFee> billFee) {
        this.billFee = billFee;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillSession getBillSession() {
        return billSession;
    }

    public void setBillSession(BillSession billSession) {
        this.billSession = billSession;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public void setRefundBillFee(List<BillFee> refundBillFee) {
        this.refundBillFee = refundBillFee;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
        System.out.println("Setting Tab Id : " + patientTabId);
        System.out.println("this.patientTabId = " + this.patientTabId);
    }

    public boolean isForiegn() {
        return foriegn;
    }

    public void setForiegn(boolean foriegn) {
        this.foriegn = foriegn;
    }

    public void validateAgentBalance() {
        fetchRecentChannelBooks(institution);
        activeCreditLimitPannel = false;

        if (errorCheckAgentValidate()) {
            activeCreditLimitPannel = true;
            return;
        }

        if (isForiegn()) {
            if (bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFfee() > (institution.getBallance() + institution.getAllowedCredit())) {
                UtilityController.addErrorMessage("Please Increase Credit Limit or Balance");
                activeCreditLimitPannel = true;
                return;
            }
        }

        if (!isForiegn()) {
            if (bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFee() > (institution.getBallance() + institution.getAllowedCredit())) {
                UtilityController.addErrorMessage("Please Increase Credit Limit or Balance");
                activeCreditLimitPannel = true;
                return;
            }
        }
        setAgentRefNo("");
    }

    public void fetchRecentChannelBooks(Institution ins) {
        System.err.println("in");
        String sql;
        HashMap m = new HashMap();

        sql = "select a from AgentReferenceBook a "
                + " where a.retired=false "
                + " and a.institution=:ins"
                + " and a.deactivate=false "
                + " order by a.id desc ";

        m.put("ins", ins);

        List<AgentReferenceBook> agentReferenceBooks = agentReferenceBookFacade.findBySQL(sql, m, 5);
        if (agentReferenceBooks.size() > 0) {
            ins.setAgentReferenceBooks(agentReferenceBooks);
        }
    }

//    public void updateCreditLimit() {
//        if (institution == null) {
//            UtilityController.addErrorMessage("Please Select a Agency");
//            return;
//        }
//
//        if (institution.getMaxCreditLimit() == 0.0) {
//            UtilityController.addErrorMessage("Please Enter Maximum Credit Limit.");
//            return;
//        }
//
//        if (institution.getMaxCreditLimit() < creditLimit) {
//            UtilityController.addErrorMessage("Please Enter less than Maximum Credit Limit");
//            return;
//        }
//
//        createAgentCreditLimitUpdateHistory(institution, creditLimit, HistoryType.AgentBalanceUpdateBill);
//        creditLimit = 0.0;
//    }
//
//    public void createAgentCreditLimitUpdateHistory(Institution ins, double transactionValue, HistoryType historyType) {
//        AgentHistory agentHistory = new AgentHistory();
//        agentHistory.setCreatedAt(new Date());
//        agentHistory.setCreater(getSessionController().getLoggedUser());
//        agentHistory.setBeforeBallance(ins.getBallance());
//        agentHistory.setTransactionValue(transactionValue);
//        agentHistory.setHistoryType(historyType);
//        agentHistory.setInstitution(institution);
//        agentHistoryFacade.create(agentHistory);
//    }
    public void changeListener() {
        bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFee(0.0);
        bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFfee(0.0);
        for (ItemFee f : bookingController.getSelectedServiceSession().getOriginatingSession().getItemFees()) {
            bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFee(bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFee() + f.getFee());
            bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFfee(bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFfee() + f.getFfee());
        }
        PaymentSchemeDiscount paymentSchemeDiscount = priceMatrixController.fetchPaymentSchemeDiscount(paymentScheme, paymentMethod);
        double d = 0;
        if (paymentSchemeDiscount != null) {
            for (ItemFee itmf : bookingController.getSelectedServiceSession().getOriginatingSession().getItemFees()) {
                if (itmf.getFeeType() == FeeType.OwnInstitution) {
                    if (foriegn) {
                        d += itmf.getFfee() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                    } else {
                        d += itmf.getFee() * (paymentSchemeDiscount.getDiscountPercent() / 100);
                    }

                }
            }
        }
        bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFee((bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFee() - d) * finalVariables.getVATPercentageWithAmount());
        bookingController.getSelectedServiceSession().getOriginatingSession().setTotalFfee((bookingController.getSelectedServiceSession().getOriginatingSession().getTotalFfee() - d) * finalVariables.getVATPercentageWithAmount());
    }

    public BookingController getbookingController() {
        return bookingController;
    }

    public void setbookingController(BookingController bookingController) {
        this.bookingController = bookingController;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSettleAgentRefNo() {
        return settleAgentRefNo;
    }

    public void setSettleAgentRefNo(String settleAgentRefNo) {
        this.settleAgentRefNo = settleAgentRefNo;
    }

    public String getAgentRefNo() {
        return agentRefNo;
    }

    public void setAgentRefNo(String agentRefNo) {
        this.agentRefNo = agentRefNo;
    }

    public ChannelBean getChannelBean() {
        return channelBean;
    }

    public void setChannelBean(ChannelBean channelBean) {
        this.channelBean = channelBean;
    }

    public PaymentMethod getSettlePaymentMethod() {
        return settlePaymentMethod;
    }

    public void setSettlePaymentMethod(PaymentMethod settlePaymentMethod) {
        this.settlePaymentMethod = settlePaymentMethod;
    }

    public PaymentMethod getPaymentMethod() {
        if (paymentMethod == null) {
            if (sessionController.getInstitutionPreference().getApplicationInstitution() == ApplicationInstitution.Cooperative) {
                if (sessionController.getBill() != null) {
                    paymentMethod = sessionController.getBill().getPaymentMethod();
                } else {
                    paymentMethod = sessionController.getInstitutionPreference().getChannellingPaymentMethod();
                }
            } else {
                paymentMethod = sessionController.getInstitutionPreference().getChannellingPaymentMethod();
            }
        }
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Institution getSettleInstitution() {
        return settleInstitution;
    }

    public void setSettleInstitution(Institution settleInstitution) {
        this.settleInstitution = settleInstitution;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public AgentHistoryFacade getAgentHistoryFacade() {
        return agentHistoryFacade;
    }

    public void setAgentHistoryFacade(AgentHistoryFacade agentHistoryFacade) {
        this.agentHistoryFacade = agentHistoryFacade;
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public AgentReferenceBookController getAgentReferenceBookController() {
        return agentReferenceBookController;
    }

    public void setAgentReferenceBookController(AgentReferenceBookController agentReferenceBookController) {
        this.agentReferenceBookController = agentReferenceBookController;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public PaymentMethod getCancelPaymentMethod() {
        return cancelPaymentMethod;
    }

    public void setCancelPaymentMethod(PaymentMethod cancelPaymentMethod) {
        this.cancelPaymentMethod = cancelPaymentMethod;
    }

    public PaymentMethod getRefundPaymentMethod() {
        return refundPaymentMethod;
    }

    public void setRefundPaymentMethod(PaymentMethod refundPaymentMethod) {
        this.refundPaymentMethod = refundPaymentMethod;
    }

    public int getPatientSearchTab() {
        return patientSearchTab;
    }

    public void setPatientSearchTab(int patientSearchTab) {
        this.patientSearchTab = patientSearchTab;
    }

    public boolean isSettleSucessFully() {
        return settleSucessFully;
    }

    public void setSettleSucessFully(boolean settleSucessFully) {
        this.settleSucessFully = settleSucessFully;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public double getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(double creditLimit) {
        this.creditLimit = creditLimit;
    }

    public boolean isActiveCreditLimitPannel() {
        return activeCreditLimitPannel;
    }

    public void setActiveCreditLimitPannel(boolean activeCreditLimitPannel) {
        this.activeCreditLimitPannel = activeCreditLimitPannel;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCommentR() {
        return commentR;
    }

    public void setCommentR(String commentR) {
        this.commentR = commentR;
    }

    public Institution getInstitutionOnCallAgency() {
        return institutionOnCallAgency;
    }

    public void setInstitutionOnCallAgency(Institution institutionOnCallAgency) {
        this.institutionOnCallAgency = institutionOnCallAgency;
    }

    public BillSession getBillSessionTmp() {
        return billSessionTmp;
    }

    public void setBillSessionTmp(BillSession billSessionTmp) {
        this.billSessionTmp = billSessionTmp;
    }

}
